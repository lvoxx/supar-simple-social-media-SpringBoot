package io.github.lvoxx.cloudinary_starter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for image upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Image upload request with base64-encoded data")
public class ImageUploadRequest {
    
    @NotBlank(message = "Image data is required")
    @Pattern(
        regexp = "^data:image/(jpeg|jpg|png|webp|gif);base64,.*",
        message = "Image must be base64-encoded with valid format (jpeg, jpg, png, webp, gif)"
    )
    @Schema(
        description = "Base64-encoded image data with data URI scheme",
        example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String imageData;
}