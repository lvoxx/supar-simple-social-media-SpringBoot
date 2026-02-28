package io.github.lvoxx.postgres_starter.auditing;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.ReactiveAuditorAware;

import reactor.core.publisher.Mono;

/**
 * Resolves the current auditor (user ID) from the Reactor Context.
 *
 * <p>
 * The user ID is placed into the Reactor Context by {@code starter-security}'s
 * {@code ClaimExtractionWebFilter} using the key {@code "userId"}.
 * This bean is used by Spring Data's {@code @CreatedBy} /
 * {@code @LastModifiedBy} auditing.
 * </p>
 *
 * <p>
 * If no user is in context (e.g., system background tasks), the auditor is
 * empty
 * and the field will be left as null — this is intentional.
 * </p>
 */
public class SecurityContextReactiveAuditorAware implements ReactiveAuditorAware<UUID> {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextReactiveAuditorAware.class);

    /**
     * Context key written by starter-security's ClaimExtractionWebFilter.
     */
    static final String CONTEXT_KEY_USER_ID = "userId";

    @Override
    public Mono<UUID> getCurrentAuditor() {
        return Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(CONTEXT_KEY_USER_ID)) {
                return Mono.empty();
            }
            try {
                String rawId = ctx.get(CONTEXT_KEY_USER_ID);
                return Mono.just(UUID.fromString(rawId));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid userId in Reactor Context for auditing: {}", e.getMessage());
                return Mono.empty();
            }
        });
    }
}