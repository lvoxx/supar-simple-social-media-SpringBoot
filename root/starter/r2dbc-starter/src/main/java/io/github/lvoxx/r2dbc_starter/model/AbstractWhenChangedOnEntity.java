package io.github.lvoxx.r2dbc_starter.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Abstract base entity for tracking when changes occurred
 * Contains id, createdAt, updatedAt fields
 */
@Getter
@Setter
@ToString(callSuper = true)
public abstract class AbstractWhenChangedOnEntity {

    @Id
    @Column("id")
    private UUID id;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}