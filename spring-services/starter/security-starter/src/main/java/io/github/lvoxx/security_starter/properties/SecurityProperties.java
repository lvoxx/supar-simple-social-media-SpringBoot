package io.github.lvoxx.security_starter.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the security starter.
 *
 * <p>
 * The K8S gateway validates JWT tokens and injects user claims as HTTP headers.
 * This starter reads those headers and builds a
 * {@link io.github.lvoxx.starter.security.model.UserPrincipal}
 * for every request.
 * </p>
 */
@ConfigurationProperties(prefix = "xsocial.security")
public class SecurityProperties {

    /** Header name carrying the authenticated user's ID (ULID). */
    private String userIdHeader = "X-User-Id";

    /** Header name carrying a comma-separated list of the user's roles. */
    private String rolesHeader = "X-User-Roles";

    /** Header name carrying the original client IP (from K8S ingress). */
    private String ipHeader = "X-Forwarded-For";

    /**
     * Paths that do not require an authenticated user.
     * Requests to these paths will proceed even without {@code X-User-Id}.
     */
    private List<String> anonymousPaths = List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**");

    public String getUserIdHeader() {
        return userIdHeader;
    }

    public void setUserIdHeader(String userIdHeader) {
        this.userIdHeader = userIdHeader;
    }

    public String getRolesHeader() {
        return rolesHeader;
    }

    public void setRolesHeader(String rolesHeader) {
        this.rolesHeader = rolesHeader;
    }

    public String getIpHeader() {
        return ipHeader;
    }

    public void setIpHeader(String ipHeader) {
        this.ipHeader = ipHeader;
    }

    public List<String> getAnonymousPaths() {
        return anonymousPaths;
    }

    public void setAnonymousPaths(List<String> anonymousPaths) {
        this.anonymousPaths = anonymousPaths;
    }
}
