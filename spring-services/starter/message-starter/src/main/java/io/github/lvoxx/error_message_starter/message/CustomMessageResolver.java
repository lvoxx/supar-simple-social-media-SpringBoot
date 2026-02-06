package io.github.lvoxx.error_message_starter.message;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomMessageResolver {

    private final MessageSource errorMessageSource;

    public String get(String code, Object... args) {
        return errorMessageSource.getMessage(
                code,
                args,
                LocaleContextHolder.getLocale()
        );
    }
}