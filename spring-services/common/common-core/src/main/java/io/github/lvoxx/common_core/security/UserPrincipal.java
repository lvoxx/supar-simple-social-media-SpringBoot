package io.github.lvoxx.common_core.security;

import java.util.Set;
import java.util.UUID;

import io.github.lvoxx.common_core.enums.UserRole;

public record UserPrincipal(
    UUID userId,
    String username,
    Set<UserRole> roles,
    String ip
) {
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return roles.contains(UserRole.ADMIN);
    }

    public boolean isModerator() {
        return roles.contains(UserRole.ADMIN) || roles.contains(UserRole.MODERATOR);
    }
}
