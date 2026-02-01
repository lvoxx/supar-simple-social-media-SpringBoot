package io.github.lvoxx.user_service.model;

import java.time.LocalDate;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.common_core.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * User entity representing user profile information.
 * Extends BaseEntity for audit fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Table("users")
public class User extends BaseEntity {

    @Column("keycloak_user_id")
    private String keycloakUserId;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("display_name")
    private String displayName;

    @Column("bio")
    private String bio;

    @Column("avatar_url")
    private String avatarUrl;

    @Column("cover_image_url")
    private String coverImageUrl;

    @Column("birth_date")
    private LocalDate birthDate;

    @Column("location")
    private String location;

    @Column("website")
    private String website;

    @Column("is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column("is_private")
    @Builder.Default
    private Boolean isPrivate = false;

    @Column("follower_count")
    @Builder.Default
    private Long followerCount = 0L;

    @Column("following_count")
    @Builder.Default
    private Long followingCount = 0L;

    @Column("post_count")
    @Builder.Default
    private Long postCount = 0L;
}