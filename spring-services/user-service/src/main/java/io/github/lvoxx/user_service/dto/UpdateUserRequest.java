package io.github.lvoxx.user_service.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update user profile")
public class UpdateUserRequest {
    
    @Size(min = 1, max = 50, message = "Display name must be between 1 and 50 characters")
    @Schema(description = "Display name", example = "John Doe")
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
    
    @Schema(description = "Whether profile is private", example = "false")
    private Boolean isPrivate;
}
