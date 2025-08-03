package com.tarkhangurbanli.libcommonlogger.annotation;

import com.tarkhangurbanli.libcommonlogger.config.LoggingRegistrar;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Enables method-level logging for the specified base package.
 * <p>
 * This annotation should be placed on your main configuration class
 * (usually the class annotated with {@code @SpringBootApplication}).
 * </p>
 * <p>
 * It registers a logging aspect that intercepts all method calls within the given package.
 * </p>
 *
 * <pre>
 * {@code
 * @EnableLogging(basePackage = "com.example.service")
 * @SpringBootApplication
 * public class MyApp {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApp.class, args);
 *     }
 * }
 * }
 * </pre>
 * <p>
 * or
 * </p>
 * <pre>
 * {@code
 * @EnableLogging(basePackage = "com.example.service")
 * public class LibConfiguration {
 * }
 * }
 * </pre>
 *
 * @author Tarkhan
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoggingRegistrar.class)
public @interface EnableLogging {

    /**
     * Base package for scanning classes whose methods will be logged.
     *
     * @return the root package name
     */
    String basePackage();
}
