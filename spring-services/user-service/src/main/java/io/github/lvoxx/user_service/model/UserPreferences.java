package io.github.lvoxx.user_service.model;

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
 * User preferences entity for privacy, notification, and content settings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@Table("user_preferences")
public class UserPreferences implements Persistable<Long> {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    // Privacy Settings
    @Column("show_email")
    private Boolean showEmail;

    @Column("show_birth_date")
    private Boolean showBirthDate;

    @Column("allow_tagging")
    private Boolean allowTagging;

    @Column("allow_mentions")
    private Boolean allowMentions;

    // Notification Settings
    @Column("notify_new_follower")
    private Boolean notifyNewFollower;

    @Column("notify_post_like")
    private Boolean notifyPostLike;

    @Column("notify_comment")
    private Boolean notifyComment;

    @Column("notify_mention")
    private Boolean notifyMention;

    @Column("notify_message")
    private Boolean notifyMessage;

    // Content Settings
    @Column("default_post_visibility")
    private String defaultPostVisibility;

    @Column("language")
    private String language;

    @Column("timezone")
    private String timezone;

    @Column("theme")
    private String theme;

    @Column("avatar_url")
    private String avatarUrl;

    @Column("cover_image_url")
    private String coverImageUrl;

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

    public UserPreferences setAsNew() {
        this.isNew = true;
        return this;
    }

    public static UserPreferences createDefaultPreferences(Long userId) {
        return UserPreferences.builder()
                .userId(userId)
                .showEmail(false)
                .showBirthDate(false)
                .allowTagging(true)
                .allowMentions(true)
                .notifyNewFollower(true)
                .notifyPostLike(true)
                .notifyComment(true)
                .notifyMention(true)
                .notifyMessage(true)
                .defaultPostVisibility("PUBLIC")
                .language("en")
                .timezone("UTC")
                .theme("LIGHT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
                .setAsNew();
    }
}