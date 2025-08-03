package com.tarkhangurbanli.libcommonlogger.aspect;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Aspect responsible for dynamic method-level logging in Spring applications.
 * <p>
 * This aspect logs method invocations across configurable packages or, if no package is provided,
 * for Spring-managed components annotated with {@code @Component}, {@code @Service}, or {@code @RestController}.
 * <p>
 * Logging is performed at different levels:
 * <ul>
 *     <li><strong>INFO</strong>: Logs a summarized view of method arguments.</li>
 *     <li><strong>DEBUG</strong>: Logs detailed arguments and return values.</li>
 *     <li><strong>ERROR</strong>: Captures exceptions with contextual information.</li>
 * </ul>
 *
 * <p>
 * Logging behavior is controlled via the {@code logging.aspect.basePackage} system property,
 * set via the {@code @EnableLogging} annotation.
 * </p>
 *
 * <p>
 * This aspect explicitly excludes itself and known servlet/filter-related classes
 * to prevent proxying issues (e.g., {@code GenericFilterBean}, {@code RegistrationBean}).
 * </p>
 *
 * @author Tarkhan
 * @since 1.1.0
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private String basePackage;

    /**
     * Initializes the aspect and reads the base package value from system properties.
     * If the base package is empty, logs will apply to Spring-managed components.
     */
    @PostConstruct
    public void init() {
        basePackage = System.getProperty("logging.aspect.basePackage", "");
        log.info("LoggingAspect initialized with basePackage: {}", basePackage.isEmpty() ? "Spring Components" : basePackage);
    }

    /**
     * Pointcut for Spring-managed beans such as @Component, @Service, and @RestController.
     */
    @Pointcut("within(@org.springframework.stereotype.Component *) || " +
            "within(@org.springframework.stereotype.Service *) || " +
            "within(@org.springframework.web.bind.annotation.RestController *)")
    private void springManagedBeans() {}

    /**
     * Pointcut for servlet filters and registration beans to exclude from logging.
     */
    @Pointcut("within(org.springframework.web.filter.GenericFilterBean+) || " +
            "within(org.springframework.boot.web.servlet.RegistrationBean+)")
    private void excludeServletFilters() {}

    /**
     * Pointcut for classes within the logging base package.
     * This placeholder is used programmatically and filtered by {@code basePackage}.
     */
    @Pointcut("execution(* *(..)) && !selfExclusion()")
    private void withinPackage() {
        // Dynamic matching by basePackage inside advice.
    }

    /**
     * Pointcut for excluding this aspect itself from logging.
     */
    @Pointcut("within(com.tarkhangurbanli.libcommonlogger.aspect..*)")
    public void selfExclusion() {}

    /**
     * Combined pointcut for dynamic logging logic.
     * Includes methods within configured base package or Spring-managed beans,
     * and excludes servlet filters and this aspect itself.
     */
    @Pointcut("(withinPackage() || springManagedBeans()) && !excludeServletFilters() && !selfExclusion()")
    public void dynamicLoggingPointcut() {}

    /**
     * Around advice that logs method entry, arguments, return values, and exceptions.
     *
     * @param joinPoint the join point representing the method
     * @return the result of method execution
     * @throws Throwable if the underlying method throws an exception
     */
    @Around("dynamicLoggingPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Check if method is outside base package scope
        if (!basePackage.isEmpty() && !className.startsWith(basePackage)) {
            return joinPoint.proceed();
        }

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
     * AfterThrowing advice that logs exception details if method throws.
     *
     * @param joinPoint the join point where exception was thrown
     * @param e         the exception
     */
    @AfterThrowing(pointcut = "dynamicLoggingPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String rootCause = getRootCauseClassName(e);

        if (!basePackage.isEmpty() && !className.startsWith(basePackage)) return;

        if (log.isDebugEnabled()) {
            log.error("Exception in {}.{}(): cause = {}, message = {}, stacktrace:", className, methodName, rootCause, e.getMessage(), e);
        } else {
            log.error("Exception in {}.{}(): cause = {}, message = {}", className, methodName, rootCause, e.getMessage());
        }
    }

    /**
     * Generates a summary of arguments for INFO-level logging.
     *
     * @param args the method arguments
     * @return summarized string
     */
    private String summarizeArguments(Object[] args) {
        if (args == null || args.length == 0) return "no arguments";

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
                        if (field.getName().toLowerCase().contains("password") || field.getName().toLowerCase().contains("secret")) continue;
                        field.setAccessible(true);
                        summary.append(field.getName()).append("=").append(field.get(arg)).append(", ");
                    }
                } catch (Exception e) {
                    summary.append("arg").append(i).append("=").append(arg).append(", ");
                }
            }
        }
        if (!summary.isEmpty()) summary.setLength(summary.length() - 2);
        return summary.toString();
    }

    /**
     * Traverses the exception stack to get the root cause class name.
     *
     * @param throwable the top-level exception
     * @return class name of root cause
     */
    private String getRootCauseClassName(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName();
    }
}
