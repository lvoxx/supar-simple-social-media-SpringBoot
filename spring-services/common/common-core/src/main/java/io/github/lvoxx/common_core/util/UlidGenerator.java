package io.github.lvoxx.common_core.util;

import java.util.UUID;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;

public final class UlidGenerator {
    private UlidGenerator() {
    }

    /**
     * Generate ULID as string
     * Time-sortable, UUID-compatible, without hotspot
     * Example: "01HXZ7K5P3..."
     */
    public static String generate() {
        return UlidCreator.getUlid().toString();
    }

    /**
     * Generate ULID as UUID
     */
    public static UUID generateAsUUID() {
        return UlidCreator.getUlid().toUuid();
    }

    /**
     * Parse ULID string to UUID
     */
    public static UUID toUUID(String ulidString) {
        return Ulid.from(ulidString).toUuid();
    }
}
