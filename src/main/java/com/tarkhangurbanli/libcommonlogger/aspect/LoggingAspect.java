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
 * Logging behavior is controlled via the {@code logging.aspect.basePackage} system property, set via the {@code @EnableLogging} annotation.
 *
 * @author Tarkhan
 * @since 1.0.0
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
     * Pointcut for any method execution.
     */
    @Pointcut("execution(* *(..))")
    private void anyMethod() {}

    /**
     * Pointcut for Spring-managed beans (e.g., annotated with @Component, @Service, @RestController).
     */
    @Pointcut("within(@org.springframework.stereotype.Component *) || within(@org.springframework.stereotype.Service *) || within(@org.springframework.web.bind.annotation.RestController *)")
    private void springManagedBeans() {}

    /**
     * Pointcut for methods in classes under the configured base package.
     */
    @Pointcut("execution(* *(..)) && withinPackage()")
    private void methodInConfiguredPackage() {}

    /**
     * Pointcut for methods in Spring-managed components.
     */
    @Pointcut("execution(* *(..)) && springManagedBeans()")
    private void methodInSpringBeans() {}

    /**
     * Combines the base package and Spring component pointcuts to apply logging dynamically.
     */
    @Pointcut("execution(* *(..)) && (withinPackage() || springManagedBeans())")
    public void dynamicLoggingPointcut() {}

    /**
     * Excludes this aspect class from being intercepted to prevent recursion.
     */
    @Pointcut("within(com.tarkhangurbanli.libcommonlogger.aspect..*)")
    public void selfExclusion() {}

    /**
     * Placeholder pointcut used for package-level dynamic filtering.
     * Actual filtering is handled programmatically via {@code basePackage}.
     */
    @Pointcut("!selfExclusion() && execution(* *(..))")
    private void withinPackage() {
        // Dynamic implementation
    }

    /**
     * Logs method entry, arguments, exit result, and exceptions around target methods.
     * Only applies to matched methods based on the pointcuts and basePackage.
     *
     * @param joinPoint the join point providing method context
     * @return result of method execution
     * @throws Throwable any exception thrown during execution
     */
    @Around("dynamicLoggingPointcut() && !selfExclusion()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (!basePackage.isEmpty() && !className.startsWith(basePackage)) {
            return joinPoint.proceed(); // skip logging if class is outside base package
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
     * Logs exceptions thrown by methods matched by the logging pointcut.
     *
     * @param joinPoint the join point providing context
     * @param e the exception thrown by the target method
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
     * Summarizes method arguments for concise INFO-level logging.
     * If the argument is an object, tries to extract its fields using reflection.
     * Sensitive fields such as 'password' or 'secret' are skipped.
     *
     * @param args method arguments
     * @return summarized string representation
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
     * Traverses the exception stack to find the root cause class name.
     *
     * @param throwable the original exception
     * @return name of the root cause class
     */
    private String getRootCauseClassName(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName();
    }

}
