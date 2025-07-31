package com.tarkhangurbanli.libcommonlogger.config;

import com.tarkhangurbanli.libcommonlogger.properties.LoggingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingPropertiesConfig {

    @Bean
    public LoggingProperties loggingProperties() {
        return new LoggingProperties();
    }

}
