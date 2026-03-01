package io.github.lvoxx.security_starter.config;

import java.nio.file.attribute.UserPrincipal;

import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import io.github.lvoxx.security_starter.annotation.CurrentUser;
import reactor.core.publisher.Mono;

/**
 * Resolves method parameters annotated with {@link CurrentUser @CurrentUser}.
 *
 * <p>
 * Registered as a custom argument resolver in
 * {@link SecurityAutoConfiguration}.
 * Reads the {@link UserPrincipal} from the Reactor Context (placed there by
 * {@link io.github.lvoxx.starter.security.filter.ClaimExtractionWebFilter}).
 * </p>
 *
 * <p>
 * If {@code @CurrentUser(required = true)} (default) and no principal is found,
 * a 401 Unauthorized response is returned.
 * </p>
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(
            MethodParameter parameter,
            BindingContext bindingContext,
            ServerWebExchange exchange) {

        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation != null && annotation.required();

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .cast(UserPrincipal.class)
                .cast(Object.class)
                .switchIfEmpty(required
                        ? Mono.error(new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED, "Authentication required"))
                        : Mono.empty());
    }
}
