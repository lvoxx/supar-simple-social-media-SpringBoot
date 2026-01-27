package io.github.lvoxx.r2dbc_starter.model;

import org.springframework.data.relational.core.mapping.Column;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Abstract base entity for tracking who made changes
 * Extends AbstractWhenChangedOnEntity and adds createdBy, updatedBy fields
 */
@Getter
@Setter
@ToString(callSuper = true)
public abstract class AbstractWhoChangedOnEntity extends AbstractWhenChangedOnEntity {

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;
}