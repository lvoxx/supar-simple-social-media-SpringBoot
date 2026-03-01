package io.github.lvoxx.security_starter.model;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Immutable representation of the authenticated user, extracted from
 * gateway-injected headers.
 *
 * <p>
 * This record is placed into the Reactor Context by
 * {@link io.github.lvoxx.starter.security.filter.ClaimExtractionWebFilter}
 * and can be injected into handler methods via
 * {@link io.github.lvoxx.starter.security.annotation.CurrentUser @CurrentUser}.
 * </p>
 *
 * <h3>Usage in a handler:</h3>
 * 
 * <pre>{@code
 * @GetMapping("/me")
 * Mono<ApiResponse<UserResponse>> getMe(@CurrentUser UserPrincipal user) {
 *     return userService.findById(user.userId());
 * }
 * }</pre>
 *
 * @param userId    the authenticated user's ULID (stored as UUID)
 * @param roles     set of role strings (e.g. {@code "USER"},
 *                  {@code "MODERATOR"}, {@code "ADMIN"})
 * @param ip        original client IP address from {@code X-Forwarded-For}
 * @param rawUserId the original string value of the user ID header (before UUID
 *                  parsing)
 */
public record UserPrincipal(
        UUID userId,
        Set<String> roles,
        String ip,
        String rawUserId) {

    /**
     * Reactor Context key used to store and retrieve the {@link UserPrincipal}.
     */
    public static final String CONTEXT_KEY = "userPrincipal";

    /**
     * Factory method — parses the raw header strings into a fully typed principal.
     *
     * @param rawUserId value from {@code X-User-Id} header
     * @param rawRoles  value from {@code X-User-Roles} header (comma-separated)
     * @param ip        value from {@code X-Forwarded-For} header
     * @return populated UserPrincipal
     */
    public static UserPrincipal of(String rawUserId, String rawRoles, String ip) {
        UUID userId = null;
        try {
            if (rawUserId != null && !rawUserId.isBlank()) {
                userId = UUID.fromString(rawUserId);
            }
        } catch (IllegalArgumentException ignored) {
            // Non-UUID user ID — leave null; security filter will handle
        }

        Set<String> roles = Set.of();
        if (rawRoles != null && !rawRoles.isBlank()) {
            roles = Arrays.stream(rawRoles.split(","))
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }

        return new UserPrincipal(userId, roles, ip, rawUserId);
    }

    /** Convenience: check if this principal has a specific role. */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Convenience: check if this principal is an admin. */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /** Convenience: check if this principal is a moderator. */
    public boolean isModerator() {
        return hasRole("MODERATOR") || isAdmin();
    }

    /** Returns {@code true} if the userId was successfully parsed. */
    public boolean isAuthenticated() {
        return userId != null;
    }
}
