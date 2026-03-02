package io.github.lvoxx.common_core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SoftDeletableEntity extends AuditableEntity {
    private Boolean isDeleted = false;
    private Instant deletedAt;
    private UUID deletedBy;
}
