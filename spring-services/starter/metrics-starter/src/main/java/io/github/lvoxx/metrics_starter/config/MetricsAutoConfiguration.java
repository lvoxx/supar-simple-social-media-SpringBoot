package io.github.lvoxx.metrics_starter.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;

import io.github.lvoxx.metrics_starter.filter.MdcContextWebFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * Auto-configuration for observability (metrics, tracing, structured logging).
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>Prometheus metrics via Micrometer — exposed at
 * {@code /actuator/prometheus}</li>
 * <li>Distributed tracing — Zipkin exporter via OpenTelemetry bridge</li>
 * <li>Global metric tags: {@code service}, {@code env} applied to all
 * meters</li>
 * <li>{@link MdcContextWebFilter} — injects traceId/spanId/userId/requestId
 * into MDC</li>
 * </ul>
 *
 * <p>
 * All Zipkin and Prometheus configuration is done via standard Spring Boot
 * {@code management.*} properties. See the bundled {@code application.yaml}.
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class MetricsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MetricsAutoConfiguration.class);

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Adds global common tags to all meters registered in this service.
     * Tags: {@code service=<app-name>}, {@code env=<profile>}.
     */
    @Bean
    @ConditionalOnMissingBean(name = "commonTagsMeterRegistryCustomizer")
    public MeterRegistryCustomizer<MeterRegistry> commonTagsMeterRegistryCustomizer() {
        log.info("[starter-metrics] Applying global meter tags: service='{}' env='{}'", serviceName, activeProfile);
        return registry -> registry.config().commonTags(List.of(
                Tag.of("service", serviceName),
                Tag.of("env", activeProfile)));
    }

    /**
     * Registers the MDC context filter that enriches every request with tracing
     * fields.
     */
    @Bean
    @ConditionalOnMissingBean(MdcContextWebFilter.class)
    public MdcContextWebFilter mdcContextWebFilter() {
        log.info("[starter-metrics] Registering MdcContextWebFilter");
        return new MdcContextWebFilter();
    }
}
