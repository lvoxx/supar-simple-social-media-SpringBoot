package io.github.lvoxx.error_message_starter.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

@Configuration
public class LocaleResolverConfig {

    @Bean
    public AcceptHeaderLocaleContextResolver localeResolver() {
        AcceptHeaderLocaleContextResolver resolver =
                new AcceptHeaderLocaleContextResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}