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
@Table("blocked_users")
class BlockedUser {

    @Id
    private Long id;

    @Column("blocker_user_id")
    private Long blockerUserId;

    @Column("blocked_user_id")
    private Long blockedUserId;

    @Column("created_at")
    private LocalDateTime createdAt;
}