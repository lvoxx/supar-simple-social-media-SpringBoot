package io.github.lvoxx.r2dbc_cassandra_starter.model;

import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;

/**
 * Abstract base entity for tracking who made changes
 * Extends AbstractWhenChangedOnEntity and adds createdBy and updatedBy fields
 * 
 * This class provides audit fields for tracking both the timing and the actor
 * responsible for entity changes. All domain entities that need full audit
 * tracking should extend this class.
 * 
 * @author lvoxx
 * @since 1.0.0
 */
@Data
public abstract class AbstractWhoChangedOnEntity extends AbstractWhenChangedOnEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Identifier of the user who created the entity
     * This can be a username, user ID, or any string identifying the creator
     */
    @Column("created_by")
    private String createdBy;
    
    /**
     * Identifier of the user who last updated the entity
     * This can be a username, user ID, or any string identifying the modifier
     */
    @Column("updated_by")
    private String updatedBy;
}