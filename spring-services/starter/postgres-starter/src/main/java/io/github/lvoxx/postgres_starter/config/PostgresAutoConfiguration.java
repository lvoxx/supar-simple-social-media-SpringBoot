package io.github.lvoxx.postgres_starter.config;

import java.util.UUID;

import org.flywaydb.core.Flyway;
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
 * <li>R2DBC connection pool (backed by Spring Boot's own
 * {@code R2dbcAutoConfiguration})</li>
 * <li>{@link org.springframework.data.r2dbc.core.R2dbcEntityTemplate} —
 * reactive template</li>
 * <li>{@link ReactiveAuditorAware} — resolves current user from Reactor
 * Context</li>
 * <li>Flyway migration runner — executes blocking migration at application
 * startup</li>
 * </ul>
 *
 * <h3>Activation:</h3>
 * Simply add this module as a Maven dependency. No extra {@code @Enable*}
 * annotation needed.
 *
 * <h3>Flyway note:</h3>
 * Flyway uses the standard JDBC URL ({@code spring.flyway.url}) for schema
 * migrations.
 * R2DBC is used for all runtime queries. This is the recommended pattern for
 * reactive apps.
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@ConditionalOnClass({ io.r2dbc.spi.ConnectionFactory.class, Flyway.class })
@EnableConfigurationProperties(PostgresProperties.class)
@EnableR2dbcAuditing(auditorAwareRef = "reactiveAuditorAware")
@EnableR2dbcRepositories
public class PostgresAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PostgresAutoConfiguration.class);

    /**
     * Registers the auditor-aware bean that resolves current user ID from Reactor
     * Context.
     * Only registered when {@code xsocial.postgres.auditor-enabled=true} (default).
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveAuditorAware.class)
    @ConditionalOnProperty(prefix = "xsocial.postgres", name = "auditor-enabled", matchIfMissing = true)
    public ReactiveAuditorAware<UUID> reactiveAuditorAware() {
        log.info("[starter-postgres] Registering SecurityContextReactiveAuditorAware");
        return new SecurityContextReactiveAuditorAware();
    }

    /**
     * Flyway migration runner.
     *
     * <p>
     * Flyway uses a blocking JDBC connection. It runs <strong>once</strong> at
     * startup
     * before the reactive server begins accepting traffic. The standard Spring Boot
     * {@code FlywayAutoConfiguration} handles this — we only customize the logging
     * and ensure it runs before the R2DBC pool is fully warmed up.
     * </p>
     *
     * <p>
     * Actual Flyway configuration ({@code spring.flyway.*}) is handled by
     * Spring Boot's own auto-configuration. This bean is a marker / logger only.
     * </p>
     */
    @Bean
    @ConditionalOnClass(Flyway.class)
    @ConditionalOnMissingBean(name = "flywayInitializer")
    public FlywayMigrationLogger flywayMigrationLogger(
            @Value("${spring.application.name:unknown-service}") String serviceName) {
        log.info("[starter-postgres] Flyway migration runner activated for service: {}", serviceName);
        return new FlywayMigrationLogger(serviceName);
    }

    /**
     * Simple marker bean for Flyway logging. The actual migration is executed by
     * Spring Boot's {@code FlywayAutoConfiguration}.
     */
    public record FlywayMigrationLogger(String serviceName) {
    }
}
