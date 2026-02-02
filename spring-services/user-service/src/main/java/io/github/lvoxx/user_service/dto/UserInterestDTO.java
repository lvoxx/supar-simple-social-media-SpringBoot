package io.github.lvoxx.user_service.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Interest Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User interest for recommendations")
public class UserInterestDTO {

    @Schema(description = "Interest ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "User ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long userId;

    @NotBlank(message = "Interest category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    @Schema(description = "Interest category", example = "TECHNOLOGY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String interestCategory;

    @NotBlank(message = "Interest name is required")
    @Size(max = 100, message = "Interest name must not exceed 100 characters")
    @Schema(description = "Interest name", example = "Artificial Intelligence", requiredMode = Schema.RequiredMode.REQUIRED)
    private String interestName;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.0", message = "Weight must be at least 0")
    @DecimalMax(value = "10.0", message = "Weight must not exceed 10")
    @Schema(description = "Interest weight for recommendations (0-10)", example = "2.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double weight;

    @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}