package io.github.lvoxx.user_service.model;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.common_core.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Table("muted_users")
class MutedUser extends BaseEntity {

    @Column("muter_user_id")
    private Long muterUserId;

    @Column("muted_user_id")
    private Long mutedUserId;
}