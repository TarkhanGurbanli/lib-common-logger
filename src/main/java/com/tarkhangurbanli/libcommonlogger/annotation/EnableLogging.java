package com.tarkhangurbanli.libcommonlogger.annotation;

import com.tarkhangurbanli.libcommonlogger.config.LoggingRegistrar;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Annotation to enable method-level logging across a specified base package
 * or default Spring-managed components (e.g. classes annotated with {@code @Component}, {@code @Service}, etc.).
 *
 * <p>When applied to a {@code @Configuration} class, this annotation activates the
 * {@link com.tarkhangurbanli.libcommonlogger.aspect.LoggingAspect LoggingAspect}, which intercepts
 * method calls and logs execution details based on logging levels ({@code INFO}, {@code DEBUG}, {@code ERROR}).</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Configuration
 * @EnableLogging(basePackage = "com.example.service")
 * public class AppConfig {
 *     // your bean definitions
 * }
 * }</pre>
 *
 * <p>If {@code basePackage} is not provided, the aspect will automatically apply logging
 * to all Spring-managed beans annotated with:
 * <ul>
 *   <li>{@code @Component}</li>
 *   <li>{@code @Service}</li>
 *   <li>{@code @Repository}</li>
 *   <li>{@code @RestController}</li>
 * </ul>
 * </p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Logs method entry and exit with arguments and return values.</li>
 *   <li>Logs exceptions with root cause and full stack trace.</li>
 *   <li>INFO-level summary for simplified overviews.</li>
 *   <li>DEBUG-level logs for full detail (when enabled).</li>
 * </ul>
 *
 * <p>This annotation is processed by {@link LoggingRegistrar}, which registers the logging aspect dynamically
 * during Spring application context startup.</p>
 *
 * @author Tarkhan
 * @see com.tarkhangurbanli.libcommonlogger.aspect.LoggingAspect
 * @see LoggingRegistrar
 * @since 1.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoggingRegistrar.class)
public @interface EnableLogging {

    /**
     * The base package to scan for method-level logging.
     * <p>
     * If left empty, only Spring-managed components will be included in the aspect's scope.
     * <p>
     * Example: {@code "com.example.myapp.services"}
     *
     * @return the base package to scan for logging
     */
    String basePackage() default "";

}
