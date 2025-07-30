package com.tarkhangurbanli.libcommonlogger.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.tarkhangurbanli.libcommonlogger.config.DataSourceProxyConfig;
import com.tarkhangurbanli.libcommonlogger.properties.SqlLoggingProperties;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Custom annotation to enable SQL query logging in Spring Boot applications.
 *
 * <p>When this annotation is placed on a Spring configuration class, it:
 * <ul>
 *   <li>Enables binding of {@link SqlLoggingProperties} to external configuration properties prefixed with "spring.jpa.sql-logging".</li>
 *   <li>Imports {@link DataSourceProxyConfig}, which wraps the primary {@link javax.sql.DataSource} in a proxy that intercepts and logs SQL queries.</li>
 * </ul>
 *
 * <p>This annotation provides a convenient way to activate detailed SQL logging,
 * including query text and optionally query parameters, based on application properties.
 * It supports safe operation by conditionally enabling logging only in non-production profiles.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * @Configuration
 * @EnableSqlLogging
 * public class AppConfig {
 *     // Configuration beans
 * }
 * }
 * </pre>
 *
 * @author Tarkhan Gurbanli
 * @since 1.0.0
 */
@Target(TYPE)
@Retention(RUNTIME)
@EnableConfigurationProperties(SqlLoggingProperties.class)
@Import(DataSourceProxyConfig.class)
public @interface EnableSqlLogging {
}
