package io.github.lvoxx.common_core.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base entity class for all domain entities.
 * Provides common fields: id, createdAt, updatedAt
 * 
 * @usage Extend this class for all entities that need audit fields
 * @reusable Yes - can be used by all services
 */
@Data
@NoArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

    @Id
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Helper method to check if entity is new (not persisted yet)
     */
    public boolean isNew() {
        return this.id == null;
    }
}