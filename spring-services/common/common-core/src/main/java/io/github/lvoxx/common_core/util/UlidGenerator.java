package io.github.lvoxx.common_core.util;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;

import lombok.RequiredArgsConstructor;

/**
 * Thread-safe ULID generator.
 * Produces time-sortable, UUID-compatible identifiers.
 */
@RequiredArgsConstructor
@Component
public final class UlidGenerator {

    private final ULID ulid;

    /**
     * Generate a new ULID string (26 characters, Crockford Base32).
     * Example: {@code "01HXZ8R2J4K5M6N7P8Q9R0S1T2"}
     */
    public String generate() {
        return ulid.nextULID();
    }

    /**
     * Generate a new ULID and convert it to a {@link UUID}.
     * Suitable for storage in PostgreSQL / Cassandra UUID columns.
     */
    public UUID generateAsUUID() {
        ULID.Value value = ulid.nextValue();
        return new UUID(value.getMostSignificantBits(), value.getLeastSignificantBits());
    }

    /**
     * Parse a ULID string and convert it to a {@link UUID}.
     */
    public static UUID toUUID(String ulid) {
        ULID.Value value = ulid.parseULID(ulid);
        return new UUID(value.getMostSignificantBits(), value.getLeastSignificantBits());
    }
}
