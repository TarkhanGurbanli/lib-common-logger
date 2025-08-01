package com.tarkhangurbanli.libcommonlogger.config;

import com.tarkhangurbanli.libcommonlogger.annotation.EnableLogging;
import com.tarkhangurbanli.libcommonlogger.aspect.LoggingAspect;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * Registers the {@link LoggingAspect} bean into the Spring context dynamically
 * when the {@link EnableLogging} annotation is used.
 *
 * <p>This registrar reads the {@code basePackage} attribute from {@code @EnableLogging},
 * sets it as a system property, and ensures that the logging aspect is properly registered.</p>
 *
 * <p>If no package is specified, the aspect will default to logging only Spring-managed components.</p>
 *
 * <p>This class implements both {@link ImportBeanDefinitionRegistrar} and {@link EnvironmentAware}
 * to interact with Spring's configuration metadata and environment.</p>
 *
 * <p>It does not require {@code @Component} because it is registered via {@code @Import}.</p>
 *
 * @author Tarkhan
 * @since 1.0.0
 */
@Getter
public class LoggingRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    /**
     * The Spring environment injected by the container.
     * Can be used for advanced conditional logic or property resolution.
     */
    private Environment environment;

    /**
     * Registers the {@link LoggingAspect} as a Spring bean.
     * Also sets the base package (if provided via annotation) as a system property.
     *
     * @param metadata the annotation metadata of the class annotated with {@link EnableLogging}
     * @param registry the bean definition registry where the aspect is registered
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableLogging.class.getName());
        String basePackage = (String) Objects.requireNonNull(attrs).get("basePackage");

        if (StringUtils.hasText(basePackage)) {
            System.setProperty("logging.aspect.basePackage", basePackage);
        } else {
            System.setProperty("logging.aspect.basePackage", "");
        }

        registry.registerBeanDefinition("loggingAspect",
                org.springframework.beans.factory.support.BeanDefinitionBuilder
                        .genericBeanDefinition(LoggingAspect.class)
                        .getBeanDefinition());
    }

    /**
     * Called by the Spring container to inject the {@link Environment}.
     *
     * @param environment the environment object containing application properties
     */
    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

}
