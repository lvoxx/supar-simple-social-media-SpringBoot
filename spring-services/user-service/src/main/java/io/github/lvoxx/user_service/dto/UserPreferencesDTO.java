package io.github.lvoxx.user_service.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Preferences Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User preferences and settings")
public class UserPreferencesDTO {

    @Schema(description = "Preference ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "User ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long userId;

    // Privacy Settings
    @NotNull(message = "Show email setting is required")
    @Schema(description = "Whether to show email publicly", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean showEmail;

    @NotNull(message = "Show birth date setting is required")
    @Schema(description = "Whether to show birth date publicly", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean showBirthDate;

    @NotNull(message = "Allow tagging setting is required")
    @Schema(description = "Whether to allow tagging", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean allowTagging;

    @NotNull(message = "Allow mentions setting is required")
    @Schema(description = "Whether to allow mentions", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean allowMentions;

    // Notification Settings
    @NotNull(message = "Notify new follower setting is required")
    @Schema(description = "Notify on new follower", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean notifyNewFollower;

    @NotNull(message = "Notify post like setting is required")
    @Schema(description = "Notify on post like", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean notifyPostLike;

    @NotNull(message = "Notify comment setting is required")
    @Schema(description = "Notify on comment", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean notifyComment;

    @NotNull(message = "Notify mention setting is required")
    @Schema(description = "Notify on mention", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean notifyMention;

    @NotNull(message = "Notify message setting is required")
    @Schema(description = "Notify on message", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean notifyMessage;

    // Content Settings
    @NotNull(message = "Default post visibility is required")
    @Pattern(regexp = "PUBLIC|FOLLOWERS|PRIVATE", message = "Visibility must be PUBLIC, FOLLOWERS, or PRIVATE")
    @Schema(description = "Default post visibility", example = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
            "PUBLIC", "FOLLOWERS", "PRIVATE" })
    private String defaultPostVisibility;

    @NotNull(message = "Language is required")
    @Schema(description = "Preferred language", example = "en", requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @NotNull(message = "Timezone is required")
    @Schema(description = "User timezone", example = "UTC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String timezone;

    @NotNull(message = "Theme is required")
    @Pattern(regexp = "LIGHT|DARK|AUTO", message = "Theme must be LIGHT, DARK, or AUTO")
    @Schema(description = "UI theme", example = "LIGHT", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
            "LIGHT", "DARK", "AUTO" })
    private String theme;

    @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-01T00:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}