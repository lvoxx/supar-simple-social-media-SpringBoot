package io.github.lvoxx.cloudinary_starter.dto;

import io.github.lvoxx.postservice.domain.entity.PostMedia.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for media data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    private Long id;
    private String mediaUrl;
    private MediaType mediaType;
    private Integer displayOrder;
    private Integer width;
    private Integer height;
    private Long fileSize;
}