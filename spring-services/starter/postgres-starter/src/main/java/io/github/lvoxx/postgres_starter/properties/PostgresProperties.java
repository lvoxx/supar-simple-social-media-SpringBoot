package io.github.lvoxx.postgres_starter.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the postgres starter.
 * All properties are optional — sane defaults are provided.
 *
 * <p>
 * Backed by {@code spring.r2dbc.*} and {@code spring.flyway.*} standard Spring
 * Boot properties.
 * This class only adds XSocial-specific extensions.
 * </p>
 */
@ConfigurationProperties(prefix = "xsocial.postgres")
public class PostgresProperties {

    /**
     * Whether to enable the ReactiveAuditorAware bean.
     * When enabled, createdBy / updatedBy are auto-populated from the current
     * user's JWT.
     */
    private boolean auditorEnabled = true;

    /**
     * Header name from which the current user ID is extracted
     * (injected by the K8S gateway).
     */
    private String userIdHeader = "X-User-Id";

    /**
     * Maximum time a query is allowed to run before timing out.
     */
    private Duration queryTimeout = Duration.ofSeconds(10);

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public boolean isAuditorEnabled() {
        return auditorEnabled;
    }

    public void setAuditorEnabled(boolean auditorEnabled) {
        this.auditorEnabled = auditorEnabled;
    }

    public String getUserIdHeader() {
        return userIdHeader;
    }

    public void setUserIdHeader(String userIdHeader) {
        this.userIdHeader = userIdHeader;
    }

    public Duration getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(Duration queryTimeout) {
        this.queryTimeout = queryTimeout;
    }
}
