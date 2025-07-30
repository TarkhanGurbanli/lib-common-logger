package com.tarkhangurbanli.libcommonlogger.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "logging.common")
public class LoggingProperties {

    // Getter and Setter
    /**
     * Log level for method logging: TRACE, DEBUG, INFO, WARN, ERROR.
     * Default is INFO.
     */
    private String level = "INFO";

    /**
     * Comma-separated list of packages to exclude from logging.
     */
    private String excludePackages = "";

}
