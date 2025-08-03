package com.tarkhangurbanli.libcommonlogger.aspect;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect responsible for dynamic method-level logging in Spring applications.
 *
 * <p>This aspect applies logging only to methods declared in classes under the specified
 * base package provided via {@code @EnableLogging(basePackage = "...")}. No reliance
 * on Spring annotations like {@code @Component}, {@code @Service}, etc.</p>
 *
 * <p>Logging is performed at multiple levels:</p>
 * <ul>
 *     <li><strong>INFO</strong>: Logs a summarized view of method arguments.</li>
 *     <li><strong>DEBUG</strong>: Logs detailed arguments and return values.</li>
 *     <li><strong>ERROR</strong>: Captures exceptions with contextual information.</li>
 * </ul>
 *
 * <p>The base package is configured via the {@code logging.aspect.basePackage} system property,
 * which is automatically set by the {@code @EnableLogging} annotation.</p>
 *
 * <p>This aspect also excludes known Spring internals and itself to prevent proxy issues.</p>
 *
 * @author Tarkhan
 * @since 1.2.0
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private String basePackage;

    /**
     * Reads the base package from system property and initializes the aspect.
     */
    @PostConstruct
    public void init() {
        basePackage = System.getProperty("logging.aspect.basePackage", "");
        log.info("LoggingAspect initialized for basePackage: {}", basePackage);
    }

    /**
     * Pointcut that matches all methods, to be filtered dynamically at runtime.
     */
    @Pointcut("execution(* *(..)) && !selfExclusion()")
    private void allMethods() {
    }

    /**
     * Pointcut to exclude this aspect class itself from being logged.
     */
    @Pointcut("within(com.tarkhangurbanli.libcommonlogger.aspect..*)")
    public void selfExclusion() {
    }

    /**
     * Pointcut to exclude known Spring servlet/filter internals from being logged.
     */
    @Pointcut("within(org.springframework.web.filter.GenericFilterBean+) || " +
            "within(org.springframework.boot.web.servlet.RegistrationBean+)")
    private void excludeServletFilters() {
    }

    /**
     * Pointcut to exclude Spring Data internal proxy fragments from logging.
     */
    @Pointcut("within(org.springframework.data.repository.core.support.RepositoryComposition+)")
    private void excludeSpringDataInternals() {
    }

    /**
     * Combined pointcut that dynamically logs methods based on the configured base package.
     */
    @Pointcut("allMethods() && !excludeServletFilters() && !excludeSpringDataInternals()")
    public void dynamicLoggingPointcut() {
    }

    /**
     * Around advice that logs method entry, arguments, return values, and exceptions.
     *
     * @param joinPoint the join point representing the method call
     * @return the method result
     * @throws Throwable if the target method throws
     */
    @Around("dynamicLoggingPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();

        if (!basePackage.isEmpty() && !className.startsWith(basePackage)) {
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (log.isInfoEnabled()) {
            log.info("Executing: {}.{}() with args summary: {}", className, methodName, summarizeArguments(args));
        }

        if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}() with full arguments: {}", className, methodName, Arrays.deepToString(args));
        }

        try {
            Object result = joinPoint.proceed();
            if (log.isDebugEnabled()) {
                log.debug("Exit: {}.{}() with result: {}", className, methodName, result);
            }
            return result;
        } catch (Throwable ex) {
            log.error("Exception in {}.{}(): {}", className, methodName, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Logs exceptions thrown from methods in the target package.
     *
     * @param joinPoint the join point where the exception occurred
     * @param e         the exception
     */
    @AfterThrowing(pointcut = "dynamicLoggingPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        if (!basePackage.isEmpty() && !className.startsWith(basePackage)) {
            return;
        }

        String methodName = joinPoint.getSignature().getName();
        String rootCause = getRootCauseClassName(e);

        if (log.isDebugEnabled()) {
            log.error("Exception in {}.{}(): cause = {}, message = {}, stacktrace:", className, methodName, rootCause, e.getMessage(),
                    e);
        } else {
            log.error("Exception in {}.{}(): cause = {}, message = {}", className, methodName, rootCause, e.getMessage());
        }
    }

    /**
     * Produces a simplified summary of arguments to be used in info-level logs.
     *
     * @param args the arguments passed to the method
     * @return a string summary of argument values
     */
    private String summarizeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "no arguments";
        }

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                summary.append("arg").append(i).append("=null, ");
            } else if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                summary.append("arg").append(i).append("=").append(arg).append(", ");
            } else {
                try {
                    Field[] fields = arg.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getName().toLowerCase().contains("password") || field.getName().toLowerCase().contains("secret")) {
                            continue;
                        }
                        field.setAccessible(true);
                        summary.append(field.getName()).append("=").append(field.get(arg)).append(", ");
                    }
                } catch (Exception e) {
                    summary.append("arg").append(i).append("=").append(arg).append(", ");
                }
            }
        }
        if (!summary.isEmpty()) {
            summary.setLength(summary.length() - 2);
        }
        return summary.toString();
    }

    /**
     * Recursively finds the root cause of the exception.
     *
     * @param throwable the exception
     * @return the root cause class name
     */
    private String getRootCauseClassName(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName();
    }
    
}
