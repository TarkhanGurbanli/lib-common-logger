package com.tarkhangurbanli.libcommonlogger.aspect;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Aspect
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut that matches all Spring-managed beans within the
     * 'com.tarkhangurbanli' package and its sub-packages,
     * excluding any beans defined within the 'com.tarkhangurbanli.libcommonlogger.aspect' package
     * and its sub-packages.
     *
     * <p>This pointcut is used to define the scope where logging aspect advices will be applied.
     * It ensures that all application beans are targeted except the aspect classes themselves,
     * preventing potential recursive calls or infinite loops.</p>
     *
     * <p>The method is intentionally left empty because it only serves as a named pointcut expression
     * for use in advice annotations such as {@code @Before}, {@code @After}, or {@code @Around}.</p>
     */
    @Pointcut("within(com.tarkhangurbanli..*) && !within(com.tarkhangurbanli.libcommonlogger.aspect..*)")
    public void springBeanPointcut() {
        // Pointcut definition method - intentionally left blank.
    }

    /**
     * Around advice that logs the entry, exit, and exceptions of methods matched by the
     * {@code springBeanPointcut()} pointcut at DEBUG log level.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Checks if DEBUG logging is enabled; if not, it proceeds with the method execution without logging for performance optimization.</li>
     *   <li>Extracts the class name, method name, and argument values of the intercepted method.</li>
     *   <li>Logs the method entry with the class name, method name, and arguments.</li>
     *   <li>Proceeds to invoke the target method and captures the returned result.</li>
     *   <li>Logs the method exit along with the result returned.</li>
     *   <li>Catches any {@link IllegalArgumentException} thrown by the method, logs the error details, and rethrows the exception to maintain behavior.</li>
     * </ol>
     *
     * <p>This advice enables detailed tracing of method calls to assist in debugging while
     * avoiding unnecessary overhead when DEBUG logging is disabled.</p>
     *
     * @param joinPoint provides reflective access to the method being advised,
     *                  including method signature and arguments
     * @return the result returned by the target method
     * @throws Throwable if the target method throws any exception
     */

    @Around("springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String declaringType = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String arguments = Arrays.toString(joinPoint.getArgs());

        // INFO LEVEL
        if (log.isInfoEnabled()) {
            log.info("Executing: {}.{}()", declaringType, methodName);
        }

        // DEBUG LEVEL
        if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}() with argument[s] = {}", declaringType, methodName, arguments);
        }

        try {
            Object result = joinPoint.proceed();

            // DEBUG OUTPUT DETAIL
            if (log.isDebugEnabled()) {
                log.debug("Exit: {}.{}() with result = {}", declaringType, methodName, result);
            }

            return result;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument: {} in {}.{}()", arguments, declaringType, methodName);
            throw e;
        }
    }

    /**
     * Logs exceptions thrown by methods matched by the {@code springBeanPointcut()} pointcut.
     *
     * <p>This method is an {@code @AfterThrowing} advice that intercepts any throwable
     * raised during method execution, logs detailed information about the exception,
     * including the method where it occurred, the root cause, and the exception message.</p>
     *
     * <p>If DEBUG logging is enabled, the full stack trace along with the message
     * is logged; otherwise, only the root cause class name is logged to minimize verbosity.</p>
     *
     * @param joinPoint provides reflective access to the method where the exception was thrown
     * @param e         the exception thrown by the target method
     */
    @AfterThrowing(pointcut = "springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        String declaringType = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object exceptionCause = getExceptionCause(e);

        if (log.isDebugEnabled()) {
            log.error("Exception in {}.{}() with cause = '{}' and exception = '{}'", declaringType, methodName, exceptionCause,
                    StringUtils.hasText(e.getMessage()) ? e.getMessage() : "No message", e);
        } else {
            log.error("Exception in {}.{}() with cause = {}", declaringType, methodName, exceptionCause);
        }
    }

    /**
     * Retrieves the root cause class name of the provided throwable by recursively
     * traversing the cause chain until the deepest cause is found.
     *
     * <p>This helps identify the original exception that triggered a chain of exceptions,
     * which is valuable for debugging and error analysis.</p>
     *
     * @param throwable the exception to analyze
     * @return the class name of the root cause throwable
     */
    private Object getExceptionCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getClass().getName();
    }

}
