package io.github.lvoxx.user_service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import lombok.experimental.SuperBuilder;

/**
 * User entity representing user profile information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@Table("users")
public class User implements Persistable<Long> {
    
    @Id
    private Long id;
    
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
    private Boolean isVerified;
    
    @Column("is_private")
    private Boolean isPrivate;
    
    @Column("follower_count")
    private Long followerCount;
    
    @Column("following_count")
    private Long followingCount;
    
    @Column("post_count")
    private Long postCount;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    @Transient
    @Default
    private boolean isNew = false;
    
    @Override
    public boolean isNew() {
        return isNew || id == null;
    }
    
    public User setAsNew() {
        this.isNew = true;
        return this;
    }
}