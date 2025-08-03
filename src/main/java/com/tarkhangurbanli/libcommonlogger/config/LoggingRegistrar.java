package com.tarkhangurbanli.libcommonlogger.config;

import com.tarkhangurbanli.libcommonlogger.annotation.EnableLogging;
import com.tarkhangurbanli.libcommonlogger.aspect.LoggingAspect;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Registrar that dynamically registers the {@link LoggingAspect} when {@link EnableLogging} is present.
 * <p>
 * It extracts the {@code basePackage} attribute from the annotation and stores it
 * as a system property. This value is later read by the aspect to determine logging scope.
 * </p>
 * <p>
 * The registrar ensures that the aspect bean is registered only when the annotation is present.
 * </p>
 *
 * @author Tarkhan
 * @since 1.0.0
 */
@Getter
public class LoggingRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    /**
     * Spring environment instance, injected by the framework.
     */
    private Environment environment;

    /**
     * Registers {@link LoggingAspect} and sets system property for base package.
     *
     * @param metadata annotation metadata from {@link EnableLogging}
     * @param registry Spring's bean definition registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableLogging.class.getName());
        String basePackage = (String) Objects.requireNonNull(attrs).get("basePackage");

        // Store base package as system property for the aspect to access
        if (StringUtils.hasText(basePackage)) {
            System.setProperty("logging.aspect.basePackage", basePackage);
        } else {
            System.setProperty("logging.aspect.basePackage", "");
        }

        // Register LoggingAspect as a Spring bean
        registry.registerBeanDefinition("loggingAspect",
                BeanDefinitionBuilder.genericBeanDefinition(LoggingAspect.class).getBeanDefinition());
    }

    /**
     * Sets the Spring environment for later use.
     *
     * @param environment the current environment
     */
    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

}
