package io.github.lvoxx.user_service.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new user")
public class CreateUserRequest {

    @NotBlank(message = "Keycloak user ID is required")
    @Schema(description = "Keycloak user ID", example = "550e8400-e29b-41d4-a716-446655440001", requiredMode = Schema.RequiredMode.REQUIRED)
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
}
