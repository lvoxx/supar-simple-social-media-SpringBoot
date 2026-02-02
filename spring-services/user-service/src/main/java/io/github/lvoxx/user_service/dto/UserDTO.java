package io.github.lvoxx.user_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User profile information")
public class UserDTO {

    @Schema(description = "User ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Keycloak user ID", example = "550e8400-e29b-41d4-a716-446655440001", accessMode = Schema.AccessMode.READ_ONLY)
    private String keycloakUserId;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain alphanumeric characters and underscores")
    @Schema(description = "Unique username", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "User email", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 50, message = "Display name must be between 1 and 50 characters")
    @Schema(description = "Display name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    @Schema(description = "User biography", example = "Software Engineer | Coffee Lover")
    private String bio;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    @Schema(description = "Cover image URL", example = "https://example.com/cover.jpg")
    private String coverImageUrl;

    @Past(message = "Birth date must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Birth date", example = "1990-01-01")
    private LocalDate birthDate;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    @Schema(description = "User location", example = "San Francisco, CA")
    private String location;

    @Size(max = 200, message = "Website must not exceed 200 characters")
    @Schema(description = "Website URL", example = "https://johndoe.com")
    private String website;

    @Schema(description = "Whether user is verified", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean isVerified;

    @Schema(description = "Whether profile is private", example = "false")
    private Boolean isPrivate;

    @Min(value = 0, message = "Follower count must be non-negative")
    @Schema(description = "Number of followers", example = "100", accessMode = Schema.AccessMode.READ_ONLY)
    private Long followerCount;

    @Min(value = 0, message = "Following count must be non-negative")
    @Schema(description = "Number of following", example = "50", accessMode = Schema.AccessMode.READ_ONLY)
    private Long followingCount;

    @Min(value = 0, message = "Post count must be non-negative")
    @Schema(description = "Number of posts", example = "25", accessMode = Schema.AccessMode.READ_ONLY)
    private Long postCount;

    @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-01T00:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}