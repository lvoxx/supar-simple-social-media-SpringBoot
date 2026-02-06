package io.github.lvoxx.cloudinary_starter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for image upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Image upload response with secure URL")
public class ImageUploadResponse {
    
    @Schema(
        description = "Secure HTTPS URL of the uploaded image",
        example = "https://res.cloudinary.com/demo/image/upload/v1234567890/user-service/avatars/123.jpg"
    )
    private String imageUrl;
    
    @Schema(
        description = "Cloudinary public ID for future operations",
        example = "user-service/avatars/123"
    )
    private String publicId;
    
    @Schema(
        description = "Image type (avatar or cover)",
        example = "avatar"
    )
    private String imageType;
}