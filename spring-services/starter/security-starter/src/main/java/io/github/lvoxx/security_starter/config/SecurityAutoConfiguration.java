package io.github.lvoxx.security_starter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import io.github.lvoxx.security_starter.filter.ClaimExtractionWebFilter;

/**
 * Auto-configuration for stateless header-based security (Spring Boot 4.0.2 /
 * Spring Security 7.x).
 *
 * <h3>Authentication model:</h3>
 * <ol>
 * <li>Clients authenticate with Keycloak and receive short-lived JWT access
 * tokens.</li>
 * <li>The K8S Ingress gateway validates JWT signatures and expiry.</li>
 * <li>The gateway injects {@code X-User-Id}, {@code X-User-Roles}, and
 * {@code X-Forwarded-For} into the upstream request to every service.</li>
 * <li>Services never deal with raw JWTs -- only these trusted headers.</li>
 * </ol>
 *
 * <h3>Spring Boot 4 / Spring Security 7 changes applied here:</h3>
 * <ul>
 * <li>{@code @EnableWebFluxSecurity} is <strong>NOT</strong> used. In SB4,
 * Spring Boot's
 * reactive security auto-configuration
 * ({@code ReactiveSecurityAutoConfiguration}) activates
 * automatically when {@code spring-boot-starter-security} is on the classpath
 * with WebFlux.
 * Adding {@code @EnableWebFluxSecurity} on top would create a
 * double-configuration conflict.
 * Instead, we simply declare a {@link SecurityWebFilterChain} bean to override
 * the default.</li>
 * <li>{@code WebSecurityConfigurerAdapter} -- fully removed in Spring Security
 * 6 / SB3.
 * The lambda DSL on {@link ServerHttpSecurity} is the correct approach.</li>
 * <li>Lambda DSL method signatures in {@link ServerHttpSecurity} are unchanged
 * in SS7.</li>
 * <li>JSpecify {@code @Nullable} / {@code @NonNull} annotations appear in
 * Spring Security 7's
 * API -- no action required here.</li>
 * </ul>
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>{@link ClaimExtractionWebFilter} -- extracts {@code UserPrincipal} from
 * headers (order=-100)</li>
 * <li>{@link CurrentUserArgumentResolver} -- resolves {@code @CurrentUser} in
 * handler methods</li>
 * <li>{@link SecurityWebFilterChain} -- permissive SS7 config (actual auth via
 * filter)</li>
 * <li>{@link io.github.lvoxx.starter.security.util.ReactiveContextUtil} --
 * static helper for
 * service-layer reactive chains</li>
 * </ul>
 */
@AutoConfiguration(before = ReactiveSecurityContextHolderAutoConfiguration.class)
@ConditionalOnClass({ SecurityWebFilterChain.class, ClaimExtractionWebFilter.class })
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration implements WebFluxConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    private final SecurityProperties properties;

    public SecurityAutoConfiguration(SecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * Spring Security 7 reactive filter chain.
     *
     * <p>
     * Configured to be <strong>fully permissive at the Spring Security
     * layer</strong>.
     * Authentication and authorization are enforced by:
     * </p>
     * <ul>
     * <li>{@link ClaimExtractionWebFilter} -- validates {@code X-User-Id}
     * presence</li>
     * <li>Service-layer ownership checks and role assertions</li>
     * </ul>
     *
     * <p>
     * CSRF is disabled (stateless JWT-based API; no browser sessions).
     * Form login and HTTP Basic are disabled (machine-to-machine API).
     * </p>
     *
     * <p>
     * <strong>Important:</strong> We do NOT use {@code @EnableWebFluxSecurity} in
     * SB4.
     * Spring Boot's {@code ReactiveSecurityAutoConfiguration} handles the WebFlux
     * security
     * setup automatically when this {@link SecurityWebFilterChain} bean is present.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(SecurityWebFilterChain.class)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Anonymous paths (actuator, docs) permitted without X-User-Id
                        .pathMatchers(properties.getAnonymousPaths().toArray(String[]::new))
                        .permitAll()
                        // All other paths: Spring Security permits, ClaimExtractionWebFilter enforces
                        .anyExchange().permitAll())
                .build();
    }

    /**
     * Core filter -- extracts UserPrincipal from gateway-injected headers.
     * Runs at {@link ClaimExtractionWebFilter#ORDER} = -100, before all business
     * filters.
     */
    @Bean
    @ConditionalOnMissingBean(ClaimExtractionWebFilter.class)
    public ClaimExtractionWebFilter claimExtractionWebFilter() {
        log.info("[starter-security] Registering ClaimExtractionWebFilter (order={})",
                ClaimExtractionWebFilter.ORDER);
        return new ClaimExtractionWebFilter(properties);
    }

    /**
     * Resolver for {@code @CurrentUser UserPrincipal} parameters in WebFlux handler
     * methods.
     */
    @Bean
    @ConditionalOnMissingBean(CurrentUserArgumentResolver.class)
    public CurrentUserArgumentResolver currentUserArgumentResolver() {
        log.info("[starter-security] Registering CurrentUserArgumentResolver");
        return new CurrentUserArgumentResolver();
    }

    /** Registers the argument resolver with Spring WebFlux. */
    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(currentUserArgumentResolver());
    }

    @Bean
    public SecurityStarterMarker securityStarterMarker(
            @Value("${spring.application.name:unknown-service}") String serviceName) {
        log.info("[starter-security] Activated for service='{}' anonymousPaths={}",
                serviceName, properties.getAnonymousPaths());
        return new SecurityStarterMarker(serviceName);
    }

    public record SecurityStarterMarker(String serviceName) {
    }
}
