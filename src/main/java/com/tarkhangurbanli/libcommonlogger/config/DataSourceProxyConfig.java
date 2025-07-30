package com.tarkhangurbanli.libcommonlogger.config;

import com.tarkhangurbanli.libcommonlogger.listener.QueryExecutionListenerImpl;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring configuration that provides a proxied DataSource to intercept and log
 * all SQL query executions.
 *
 * <p>This configuration is conditionally enabled when the property
 * {@code spring.jpa.sql-logging.enabled=true} is set in the application properties.
 *
 * <p>The proxied DataSource wraps the real DataSource built from {@link DataSourceProperties}
 * and attaches a {@link QueryExecutionListenerImpl} to log query details.
 *
 * <p>It also logs active Spring profiles for context.
 *
 * @author Tarkhan Gurbanli
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "spring.jpa.sql-logging",
        name = "enabled",
        havingValue = "true"
)
public class DataSourceProxyConfig {

    private final Environment environment;

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        DataSource delegate = properties.initializeDataSourceBuilder().build();
        log.info("Initializing ProxyDataSource for SQL tracing (profiles: {})",
                environment.getProperty("spring.profiles.active", ""));
        return ProxyDataSourceBuilder
                .create(delegate)
                .listener(new QueryExecutionListenerImpl(environment))
                .build();
    }

}
