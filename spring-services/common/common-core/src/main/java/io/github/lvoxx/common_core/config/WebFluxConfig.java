package io.github.lvoxx.common_core.config;

import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux configuration.
 *
 * {@code @EnableWebFlux} is intentionally omitted – it is automatically activated
 * by {@link WebFluxAutoConfiguration} when spring-boot-starter-webflux is on the classpath.
 * Adding it manually would disable Spring Boot's own auto-configuration.
 *
 * CORS is configured programmatically here; if only simple origins/methods are needed
 * the equivalent can be expressed via {@code spring.web.cors.*} properties.
 */
@EnableWebFlux
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}