package com.tarkhangurbanli.libcommonlogger.aspect;

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

@Component
@Aspect
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut that matches all Spring-managed beans annotated with {@link org.springframework.stereotype.Component}
     * (including {@code @Service}, {@code @Repository}, {@code @Controller}, etc.),
     * excluding the aspect package itself to avoid recursive logging.
     *
     * <p>This makes the aspect applicable to all components in the application context,
     * regardless of their package, ensuring broad coverage without tight coupling to a base package name.</p>
     */
    @Pointcut("within(@org.springframework.stereotype.Component *) && !within(com.tarkhangurbanli.libcommonlogger.aspect..*)")
    public void springBeanPointcut() {
    }


    /**
     * Around advice for logging method execution at different levels.
     * <p>
     * - INFO: Logs method invocation with simplified argument details (e.g., key fields like name, age).
     * - DEBUG: Logs detailed entry/exit with full arguments and results.
     * - Handles specific exceptions with ERROR logging.
     */
    @Around("springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // INFO level: Simple execution log with key argument fields
        if (log.isInfoEnabled()) {
            String argSummary = summarizeArguments(args);
            log.info("Executing: {}.{}() with args summary: {}", className, methodName, argSummary);
        }

        // DEBUG level: Detailed entry log
        if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}() with full arguments: {}", className, methodName, Arrays.deepToString(args));
        }

        try {
            Object result = joinPoint.proceed();

            // DEBUG level: Detailed exit log
            if (log.isDebugEnabled()) {
                log.debug("Exit: {}.{}() with result: {}", className, methodName, result);
            }

            return result;
        } catch (IllegalArgumentException e) {
            // ERROR level: Specific exception handling
            log.error("Illegal argument in {}.{}(): args = {}, error: {}", className, methodName, Arrays.toString(args),
                    e.getMessage(), e);
            throw e;
        } catch (Throwable t) {
            // General ERROR for other exceptions
            log.error("Unexpected error in {}.{}(): {}", className, methodName, t.getMessage(), t);
            throw t;
        }
    }

    /**
     * AfterThrowing advice for logging exceptions.
     * <p>
     * - ERROR: Logs root cause.
     * - DEBUG: Logs full stack trace.
     */
    @AfterThrowing(pointcut = "springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
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
     * Summarizes arguments for INFO level logging, extracting key fields if possible.
     * Example: For an object with fields like title, name, age, returns "title=Book1, name=John, age=30".
     * Falls back to toString() if reflection fails or for simple types.
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
                continue;
            }

            // Simple types: direct toString
            if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                summary.append("arg").append(i).append("=").append(arg).append(", ");
                continue;
            }

            // For objects: Use reflection to get fields (non-sensitive)
            try {
                Field[] fields = arg.getClass().getDeclaredFields();
                for (Field field : fields) {
                    // Skip sensitive fields (e.g., password)
                    if (field.getName().toLowerCase().contains("password") || field.getName().toLowerCase().contains("secret")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(arg);
                    summary.append(field.getName()).append("=").append(value).append(", ");
                }
            } catch (Exception ex) {
                // Fallback to toString if reflection fails
                summary.append("arg").append(i).append("=").append(arg).append(", ");
            }
        }

        // Remove trailing comma
        if (summary.length() > 0) {
            summary.setLength(summary.length() - 2);
        }
        return summary.toString();
    }

    /**
     * Gets the root cause class name of the throwable.
     */
    private String getRootCauseClassName(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName();
    }

}