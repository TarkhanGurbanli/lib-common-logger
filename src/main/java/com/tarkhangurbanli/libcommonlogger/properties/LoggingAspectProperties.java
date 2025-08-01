package com.tarkhangurbanli.libcommonlogger.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "logging.aspect")
public class LoggingAspectProperties {

    /**
     * Base package to apply logging on.
     * Example: com.example.myapp
     */
    private String basePackage;

}
