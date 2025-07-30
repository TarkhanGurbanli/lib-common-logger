package com.tarkhangurbanli.libcommonlogger.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for enabling and customizing SQL query logging.
 *
 * <p>Properties are bound from configuration files (application.properties, application.yml)
 * using the prefix {@code spring.jpa.sql-logging}.</p>
 *
 * <ul>
 *   <li>{@code enabled} - Enables or disables SQL query logging (default is false).</li>
 *   <li>{@code showParameters} - Controls whether query parameters are shown inline in the logs (default is false).</li>
 * </ul>
 *
 * <p>Example configuration:</p>
 * <pre>
 * spring.jpa.sql-logging.enabled=true
 * spring.jpa.sql-logging.show-parameters=true
 * </pre>
 *
 * @author Tarkhan Gurbanli
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.jpa.sql-logging")
public class SqlLoggingProperties {

    private boolean enabled = false;

    private boolean showParameters = false;

}
