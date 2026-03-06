package io.github.lvoxx.postgres_starter.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import io.github.lvoxx.postgres_starter.auditing.SecurityContextReactiveAuditorAware;
import io.github.lvoxx.postgres_starter.properties.PostgresProperties;

/**
 * Auto-configuration for PostgreSQL connectivity via R2DBC.
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>R2DBC reactive connection pool (via Spring Boot's
 * {@code R2dbcAutoConfiguration})</li>
 * <li>{@link org.springframework.data.r2dbc.core.R2dbcEntityTemplate} --
 * reactive CRUD template</li>
 * <li>{@link ReactiveAuditorAware} -- resolves current user from Reactor
 * Context for
 * {@code @CreatedBy} / {@code @LastModifiedBy} auditing</li>
 * </ul>
 *
 * <h3>Schema initialization -- intentionally excluded:</h3>
 * DB schema migrations are run by K8S init-containers before the service pod
 * starts.
 * No Flyway or Liquibase dependency is included in this starter.
 *
 * <h3>Auditing:</h3>
 * Entities get {@code created_by} / {@code updated_by} auto-populated from the
 * Reactor Context,
 * which is seeded by {@code security-starter}'s
 * {@code ClaimExtractionWebFilter}.
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@ConditionalOnClass(io.r2dbc.spi.ConnectionFactory.class)
@EnableConfigurationProperties(PostgresProperties.class)
@EnableR2dbcAuditing(auditorAwareRef = "reactiveAuditorAware")
@EnableR2dbcRepositories
public class PostgresAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PostgresAutoConfiguration.class);

    /**
     * Reactive auditor bean that resolves the current user's UUID from Reactor
     * Context.
     * Populated by {@code security-starter}'s {@code ClaimExtractionWebFilter} on
     * every request.
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveAuditorAware.class)
    @ConditionalOnProperty(prefix = "sssm.postgres", name = "auditor-enabled", matchIfMissing = true)
    public ReactiveAuditorAware<UUID> reactiveAuditorAware() {
        log.info("[postgres-starter] Registering SecurityContextReactiveAuditorAware");
        return new SecurityContextReactiveAuditorAware();
    }

    @Bean
    public PostgresStarterMarker postgresStarterMarker(
            @Value("${spring.application.name:unknown-service}") String serviceName) {
        log.info("[postgres-starter] Activated for service='{}'", serviceName);
        return new PostgresStarterMarker(serviceName);
    }

    public record PostgresStarterMarker(String serviceName) {
    }
}
