package io.github.lvoxx.security_starter.utils;

import java.util.UUID;

import io.github.lvoxx.security_starter.model.UserPrincipal;
import reactor.core.publisher.Mono;

/**
 * Utility class for accessing the current {@link UserPrincipal} from anywhere
 * in a reactive chain without requiring method parameter injection.
 *
 * <h3>Usage:</h3>
 * 
 * <pre>{@code
 * // In a service method:
 * return ReactiveContextUtil.getCurrentUser()
 *         .flatMap(user -> postRepository.findAllByAuthorId(user.userId()));
 *
 * // When you only need the userId:
 * return ReactiveContextUtil.getCurrentUserId()
 *         .flatMap(userId -> userService.findById(userId));
 * }</pre>
 *
 * <p>
 * If no principal is in context (anonymous request or background task),
 * the returned {@link Mono} completes empty. Use
 * {@code switchIfEmpty(Mono.error(...))}
 * to enforce authentication in service-layer code.
 * </p>
 */
public final class ReactiveContextUtil {

    private ReactiveContextUtil() {
    }

    /**
     * Returns the current {@link UserPrincipal} from the Reactor Context.
     *
     * @return {@code Mono<UserPrincipal>} — empty if no authenticated user in
     *         context
     */
    public static Mono<UserPrincipal> getCurrentUser() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(UserPrincipal.CONTEXT_KEY)) {
                return Mono.just((UserPrincipal) ctx.get(UserPrincipal.CONTEXT_KEY));
            }
            return Mono.empty();
        });
    }

    /**
     * Returns the current user's UUID ID from the Reactor Context.
     *
     * @return {@code Mono<UUID>} — empty if no authenticated user in context
     */
    public static Mono<UUID> getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::userId);
    }

    /**
     * Returns the current user's ID as a {@link String} from the Reactor Context.
     * Uses the raw string value to avoid UUID parsing overhead in simple cases.
     *
     * @return {@code Mono<String>} — empty if no authenticated user in context
     */
    public static Mono<String> getCurrentUserIdAsString() {
        return getCurrentUser().map(u -> u.userId().toString());
    }

    /**
     * Asserts that an authenticated user is present in context.
     * If no user is found, returns an error signal.
     *
     * @param errorSupplier supplier for the exception to throw if unauthenticated
     * @return the current UserPrincipal or an error
     */
    public static Mono<UserPrincipal> requireCurrentUser(
            java.util.function.Supplier<? extends Throwable> errorSupplier) {
        return getCurrentUser().switchIfEmpty(Mono.error(errorSupplier));
    }
}
