package io.github.lvoxx.media_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter uploadCounter(MeterRegistry registry) {
        return Counter.builder("media.upload.total")
                .description("Total number of media uploads")
                .register(registry);
    }

    @Bean
    public Counter viewCounter(MeterRegistry registry) {
        return Counter.builder("media.view.total")
                .description("Total number of media views")
                .register(registry);
    }

    @Bean
    public Counter deleteCounter(MeterRegistry registry) {
        return Counter.builder("media.delete.total")
                .description("Total number of media deletions")
                .register(registry);
    }
}