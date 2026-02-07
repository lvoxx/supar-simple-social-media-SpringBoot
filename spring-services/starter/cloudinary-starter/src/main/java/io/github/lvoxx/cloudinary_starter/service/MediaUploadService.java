package io.github.lvoxx.cloudinary_starter.service;

import com.cloudinary.EagerTransformation;
import com.cloudinary.Transformation;

import reactor.core.publisher.Mono;

/**
 * Base service for uploading media (images/videos) to Cloudinary.
 * 
 * Provides common upload/delete operations with reactive wrappers.
 * Subclasses can customize transformations and folder structures.
 */
public interface MediaUploadService {
    /**
     * Upload media with custom transformation.
     * 
     * @param publicId       Cloudinary public ID (path)
     * @param mediaData      Base64-encoded data
     * @param resourceType   "image" or "video"
     * @param transformation Optional Cloudinary transformation
     * @return Mono with secure URL
     */
    abstract Mono<String> uploadMedia(
            String publicId,
            String mediaData,
            String resourceType,
            Transformation<EagerTransformation> transformation);

    /**
     * Upload image with transformation.
     */
    abstract Mono<String> uploadImage(String publicId, String imageData, Transformation<EagerTransformation> transformation);

    /**
     * Upload video with transformation.
     */
    abstract Mono<String> uploadVideo(String publicId, String videoData, Transformation<EagerTransformation> transformation);

    /**
     * Delete media from Cloudinary.
     * 
     * @param publicId Cloudinary public ID
     * @return Mono<Void> when deletion completes
     */
    abstract Mono<Void> deleteMedia(String publicId);

    /**
     * Delete video from Cloudinary.
     */
    abstract Mono<Void> deleteVideo(String publicId);
}
