package io.github.lvoxx.security_starter.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.github.lvoxx.security_starter.model.UserPrincipal;
import io.github.lvoxx.security_starter.properties.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebFilter that extracts user identity from gateway-injected HTTP headers and
 * places a {@link UserPrincipal} into the Reactor Context.
 *
 * <h3>How it works:</h3>
 * <ol>
 * <li>The K8S Ingress gateway validates the JWT token from the client.</li>
 * <li>The gateway injects {@code X-User-Id}, {@code X-User-Roles}, and
 * {@code X-Forwarded-For} as headers on the upstream request.</li>
 * <li>This filter reads those headers, builds a {@link UserPrincipal}, and
 * stores it in the Reactor Context under key
 * {@link UserPrincipal#CONTEXT_KEY}.</li>
 * <li>Downstream handlers access it via {@code @CurrentUser} or
 * {@link io.github.lvoxx.starter.security.util.ReactiveContextUtil}.</li>
 * </ol>
 *
 * <h3>Anonymous paths:</h3>
 * Requests to paths matching {@code xsocial.security.anonymous-paths} patterns
 * are allowed through without a user principal. All other paths require
 * a valid {@code X-User-Id} header.
 *
 * <h3>Security note:</h3>
 * This filter trusts the injected headers completely. Header tampering
 * prevention
 * is handled at the K8S Ingress layer (headers are stripped from client
 * requests
 * and only set by the gateway after JWT validation).
 */
@Slf4j
public class ClaimExtractionWebFilter implements WebFilter, Ordered {

    /**
     * Run very early in the filter chain, before business filters.
     */
    public static final int ORDER = -100;

    private final SecurityProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ClaimExtractionWebFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        var headers = request.getHeaders();
        String path = request.getPath().value();

        String rawUserId = headers.getFirst(properties.getUserIdHeader());
        String rawRoles = headers.getFirst(properties.getRolesHeader());
        String ip = extractClientIp(headers.getFirst(properties.getIpHeader()));

        // Anonymous path — skip authentication requirement
        if (isAnonymousPath(path)) {
            return chain.filter(exchange);
        }

        // No userId header — the gateway should have rejected this, but we guard here
        // too
        if (rawUserId == null || rawUserId.isBlank()) {
            log.warn("[security-starter] Missing {} header for path='{}'",
                    properties.getUserIdHeader(), path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        UserPrincipal principal = UserPrincipal.of(rawUserId, rawRoles, ip);

        if (!principal.isAuthenticated()) {
            log.warn("[security-starter] Invalid userId='{}' for path='{}'", rawUserId, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        log.debug("[security-starter] Authenticated userId='{}' roles={} path='{}'",
                principal.userId(), principal.roles(), path);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(UserPrincipal.CONTEXT_KEY, principal));
    }

    private boolean isAnonymousPath(String path) {
        return properties.getAnonymousPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Extracts the real client IP from the X-Forwarded-For header.
     * The header may contain a comma-separated list (proxy chain);
     * the leftmost IP is the original client.
     */
    private String extractClientIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        String[] parts = forwardedFor.split(",");
        return parts[0].trim();
    }
}
