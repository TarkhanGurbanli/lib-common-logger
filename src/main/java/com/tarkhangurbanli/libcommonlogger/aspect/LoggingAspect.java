package com.tarkhangurbanli.libcommonlogger.aspect;

import com.tarkhangurbanli.libcommonlogger.properties.LoggingAspectProperties;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging method invocations on Spring-managed beans.
 *
 * <p>Provides logging at multiple levels:</p>
 * <ul>
 *   <li><strong>INFO</strong> level: Logs concise method entry with summarized arguments.</li>
 *   <li><strong>DEBUG</strong> level: Logs detailed method entry and exit including arguments and return values.</li>
 *   <li><strong>ERROR</strong> level: Logs exceptions and root cause messages.</li>
 * </ul>
 *
 * <p>The target base package for logging is configurable through the {@code logging.aspect.base-package} property.
 * If not set, all classes annotated with Spring's {@code @Component} will be logged.</p>
 *
 * @author Tarkhan
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "logging.aspect", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAspect {

    private final LoggingAspectProperties properties;
    private String effectiveBasePackage;

    /**
     * Initializes the effective base package for logging.
     * <p>
     * Reads the {@code logging.aspect.base-package} value from application properties.
     * If not provided, falls back to logging all Spring-managed components.
     */
    @PostConstruct
    public void init() {
        effectiveBasePackage = properties.getBasePackage();
        if (effectiveBasePackage == null || effectiveBasePackage.isBlank()) {
            log.warn("No base package provided. Defaulting to log all @Component classes.");
        } else {
            log.info("Logging will apply to base package: {}", effectiveBasePackage);
        }
    }

    /**
     * Pointcut that matches all Spring-managed beans annotated with {@code @Component} or its specializations.
     * <p>
     * Used as the primary filter when no specific base package is configured.
     */
    @Pointcut("execution(* *(..)) && (@within(org.springframework.stereotype.Component) || @target(org.springframework.stereotype.Component))")
    public void componentBeans() {
    }

    /**
     * Logs method entry, exit, arguments, return values, and exceptions.
     * <p>
     * Logging behavior depends on the enabled log level:
     * <ul>
     *     <li><strong>INFO</strong>: Summarized arguments only.</li>
     *     <li><strong>DEBUG</strong>: Full arguments and return value.</li>
     *     <li><strong>ERROR</strong>: Exception details.</li>
     * </ul>
     *
     * @param joinPoint the intercepted method call context
     * @return the result of the method execution
     * @throws Throwable if the target method throws an exception
     */
    @Around("componentBeans()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (log.isInfoEnabled()) {
            String summary = summarizeArguments(args);
            log.info("Executing: {}.{}() with args summary: {}", className, methodName, summary);
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
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument in {}.{}(): args = {}, error: {}", className, methodName, Arrays.toString(args),
                    e.getMessage(), e);
            throw e;
        } catch (Throwable t) {
            log.error("Unexpected error in {}.{}(): {}", className, methodName, t.getMessage(), t);
            throw t;
        }
    }

    /**
     * Logs exceptions thrown by methods matched by the pointcut.
     * <p>
     * Stack trace is logged only when DEBUG logging is enabled.
     *
     * @param joinPoint the intercepted method call
     * @param e the exception that was thrown
     */
    @AfterThrowing(pointcut = "componentBeans()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Throwable root = getRootCause(e);

        if (log.isDebugEnabled()) {
            log.error("Exception in {}.{}(): cause = {}, message = {}, stacktrace:", className, methodName, root.getClass().getName(),
                    e.getMessage(), e);
        } else {
            log.error("Exception in {}.{}(): cause = {}, message = {}", className, methodName, root.getClass().getName(),
                    e.getMessage());
        }
    }

    /**
     * Creates a summary of method arguments for concise logging.
     * <p>
     * If the argument is a simple type, its value is logged directly.
     * For objects, selected fields are extracted using reflection.
     *
     * @param args the arguments to summarize
     * @return string summary of the arguments
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
            if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                summary.append("arg").append(i).append("=").append(arg).append(", ");
                continue;
            }
            try {
                Field[] fields = arg.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.getName().toLowerCase().contains("password") || field.getName().toLowerCase().contains("secret")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(arg);
                    summary.append(field.getName()).append("=").append(value).append(", ");
                }
            } catch (Exception ex) {
                summary.append("arg").append(i).append("=").append(arg).append(", ");
            }
        }
        if (summary.length() > 0) {
            summary.setLength(summary.length() - 2);
        }
        return summary.toString();
    }

    /**
     * Extracts the root cause of a Throwable.
     *
     * @param throwable the top-level exception
     * @return the root cause in the exception chain
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

}
