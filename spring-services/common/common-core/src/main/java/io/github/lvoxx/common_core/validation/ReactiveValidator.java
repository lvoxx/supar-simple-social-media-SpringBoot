package io.github.lvoxx.common_core.validation;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lvoxx.common_core.exception.ValidationException;
import io.github.lvoxx.common_core.message.MessageKeys;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ReactiveValidator {
    private final Validator validator;

    public ReactiveValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validate object and return Mono.error(ValidationException) if there are
     * violations
     */
    public <T> Mono<T> validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if (violations.isEmpty()) {
            return Mono.just(object);
        }

        String details = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));

        return Mono.error(
                new ValidationException(MessageKeys.INTERNAL_ERROR, details));
    }
}
