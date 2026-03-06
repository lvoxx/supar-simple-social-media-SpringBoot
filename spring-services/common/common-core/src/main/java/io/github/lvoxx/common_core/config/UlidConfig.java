package io.github.lvoxx.common_core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@Scope("application") // Singleton scope is default, but explicitly stating it for clarity
public class UlidConfig {

    @Bean
    public UlidGenerator ulidGenerator() {
        return new UlidGenerator();
    }
}
