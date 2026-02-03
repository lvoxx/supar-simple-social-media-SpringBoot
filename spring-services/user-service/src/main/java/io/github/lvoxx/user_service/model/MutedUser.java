package io.github.lvoxx.user_service.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode
@Table("muted_users")
class MutedUser {

    @Id
    private Long id;

    @Column("muter_user_id")
    private Long muterUserId;

    @Column("muted_user_id")
    private Long mutedUserId;

    @Column("created_at")
    private LocalDateTime createdAt;
}