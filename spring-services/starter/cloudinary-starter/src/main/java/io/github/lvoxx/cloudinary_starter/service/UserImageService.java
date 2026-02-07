package io.github.lvoxx.cloudinary_starter.service;

import reactor.core.publisher.Mono;

/**
 * Service interface for image upload operations.
 */
public interface UserImageService {
    
    /**
     * Upload avatar image to Cloudinary.
     * 
     * @param userId User ID for folder organization
     * @param imageData Base64-encoded image data
     * @return Mono with the uploaded image URL
     */
    Mono<String> uploadAvatar(Long userId, String imageData);
    
    /**
     * Upload cover image to Cloudinary.
     * 
     * @param userId User ID for folder organization
     * @param imageData Base64-encoded image data
     * @return Mono with the uploaded image URL
     */
    Mono<String> uploadCoverImage(Long userId, String imageData);
    
    /**
     * Delete image from Cloudinary by public ID.
     * 
     * @param publicId Cloudinary public ID
     * @return Mono<Void> when deletion completes
     */
    Mono<Void> deleteImage(String publicId);
}