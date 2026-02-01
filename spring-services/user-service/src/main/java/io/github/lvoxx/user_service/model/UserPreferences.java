package io.github.lvoxx.user_service.model;

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
 * User preferences entity for privacy, notification, and content settings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Table("user_preferences")
public class UserPreferences extends BaseEntity {
    
    @Column("user_id")
    private Long userId;
    
    // Privacy Settings
    @Column("show_email")
    @Builder.Default
    private Boolean showEmail = false;
    
    @Column("show_birth_date")
    @Builder.Default
    private Boolean showBirthDate = false;
    
    @Column("allow_tagging")
    @Builder.Default
    private Boolean allowTagging = true;
    
    @Column("allow_mentions")
    @Builder.Default
    private Boolean allowMentions = true;
    
    // Notification Settings
    @Column("notify_new_follower")
    @Builder.Default
    private Boolean notifyNewFollower = true;
    
    @Column("notify_post_like")
    @Builder.Default
    private Boolean notifyPostLike = true;
    
    @Column("notify_comment")
    @Builder.Default
    private Boolean notifyComment = true;
    
    @Column("notify_mention")
    @Builder.Default
    private Boolean notifyMention = true;
    
    @Column("notify_message")
    @Builder.Default
    private Boolean notifyMessage = true;
    
    // Content Settings
    @Column("default_post_visibility")
    @Builder.Default
    private String defaultPostVisibility = "PUBLIC";
    
    @Column("language")
    @Builder.Default
    private String language = "en";
    
    @Column("timezone")
    @Builder.Default
    private String timezone = "UTC";
    
    @Column("theme")
    @Builder.Default
    private String theme = "LIGHT";
}