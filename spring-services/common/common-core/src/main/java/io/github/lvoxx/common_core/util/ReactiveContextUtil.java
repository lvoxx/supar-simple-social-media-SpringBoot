package io.github.lvoxx.common_core.util;

import java.util.UUID;

import io.github.lvoxx.common_core.security.UserPrincipal;
import reactor.core.publisher.Mono;

public final class ReactiveContextUtil {
    private ReactiveContextUtil() {
    }

    /**
     * Extract UserPrincipal from Reactor Context
     * (Propagated by security-starter)
     */
    public static Mono<UserPrincipal> getCurrentUser() {
        return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.getOrEmpty(UserPrincipal.class)));
    }

    /**
     * Extract current user ID from Reactor Context
     */
    public static Mono<UUID> getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::userId);
    }
}
