package io.github.lvoxx.cloudinary_starter.dto;

import io.github.lvoxx.postservice.domain.entity.PostMedia.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for uploading media.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadRequest {

    @NotBlank(message = "Media data is required")
    private String mediaData; // Base64 encoded

    @NotNull(message = "{validation.post.media.invalid-type}")
    private MediaType mediaType;

    private Integer displayOrder;

    private Integer width;

    private Integer height;
}