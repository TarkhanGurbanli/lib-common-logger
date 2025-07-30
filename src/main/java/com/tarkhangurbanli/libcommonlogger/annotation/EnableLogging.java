package com.tarkhangurbanli.libcommonlogger.annotation;

import com.tarkhangurbanli.libcommonlogger.aspect.LoggingAspect;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Custom annotation to enable method-level logging via AOP.
 *
 * <p>When this annotation is placed on a Spring configuration class, it activates the
 * {@link LoggingAspect}, which intercepts method calls and logs their entry and exit based
 * on configurable properties.</p>
 *
 * <p>This annotation is meta-annotated with several standard Java and Spring annotations:</p>
 *
 * <ul>
 *   <li>{@link Target}: Specifies that this annotation can only be applied to classes or interfaces.</li>
 *   <li>{@link Retention}: Indicates that this annotation will be retained at runtime, so it can be read
 *       reflectively during Spring's configuration processing.</li>
 *   <li>{@link Documented}: Ensures this annotation appears in generated JavaDocs for better visibility.</li>
 *   <li>{@link Import}: Automatically imports the {@link LoggingAspect} class into the Spring application
 *       context when this annotation is used, enabling the logging functionality.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Configuration
 * @EnableLogging
 * public class AppConfig {
 *     // Your beans and configurations
 * }
 * }
 * </pre>
 *
 * @author Tarkhan Gurbanli
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoggingAspect.class)
public @interface EnableLogging {
}
