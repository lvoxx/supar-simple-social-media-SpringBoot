package io.github.lvoxx.cloudinary_starter.service;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.EagerTransformation;
import com.cloudinary.Transformation;

import io.github.lvoxx.cloudinary_starter.properties.CloudinaryProperties;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * User-specific image upload service.
 * 
 * Handles avatar and cover image uploads with predefined transformations
 * optimized for user profiles.
 */
@Slf4j
@Service
public class UserImageUploadService extends MediaUploadService {

    private final CloudinaryProperties properties;

    public UserImageUploadService(Cloudinary cloudinary, CloudinaryProperties properties) {
        super(cloudinary);
        this.properties = properties;
    }

    /**
     * Upload avatar image with face detection and cropping.
     * 
     * Transformation:
     * - Size: 400x400px
     * - Crop: fill with face gravity
     * - Quality: auto
     * - Format: auto (WebP/JPEG)
     * 
     * @param userId    User ID for folder organization
     * @param imageData Base64-encoded image data
     * @return Mono with the uploaded image URL
     */
    public Mono<String> uploadAvatar(Long userId, String imageData) {
        String publicId = uploadFolder + "/avatars/" + userId;

        Transformation<EagerTransformation> transformation = new Transformation<EagerTransformation>()
                .width(400).height(400)
                .crop("fill")
                .gravity("face")
                .quality("auto")
                .fetchFormat("auto");

        return uploadImage(publicId, imageData, transformation);
    }

    /**
     * Upload cover image with center cropping.
     * 
     * Transformation:
     * - Size: 1500x500px
     * - Crop: fill with center gravity
     * - Quality: auto
     * - Format: auto (WebP/JPEG)
     * 
     * @param userId    User ID for folder organization
     * @param imageData Base64-encoded image data
     * @return Mono with the uploaded image URL
     */
    public Mono<String> uploadCoverImage(Long userId, String imageData) {
        String publicId = uploadFolder + "/covers/" + userId;

        Transformation<EagerTransformation> transformation = new Transformation<EagerTransformation>()
                .width(1500).height(500)
                .crop("fill")
                .gravity("center")
                .quality("auto")
                .fetchFormat("auto");

        return uploadImage(publicId, imageData, transformation);
    }

    /**
     * Delete avatar and return default avatar URL.
     * 
     * When a user deletes their avatar, this method removes the custom avatar
     * from Cloudinary and returns the default avatar URL configured in properties.
     * 
     * @param userId User ID whose avatar to delete
     * @return Mono with the default avatar URL
     */
    public Mono<String> deleteAvatar(Long userId) {
        String publicId = uploadFolder + "/avatars/" + userId;

        log.debug("Deleting avatar for user: {}", userId);

        return deleteMedia(publicId)
                .then(Mono.fromSupplier(() -> {
                    log.info("Avatar deleted for user {}, returning default avatar URL", userId);
                    return properties.defaultAvatarUrl();
                }))
                .doOnError(e -> log.error("Error deleting avatar for user {}: {}", userId, e.getMessage()));
    }

    /**
     * Delete cover image.
     * 
     * @param userId User ID whose cover image to delete
     * @return Mono<Void> when deletion completes
     */
    public Mono<Void> deleteCoverImage(Long userId) {
        String publicId = uploadFolder + "/covers/" + userId;

        log.debug("Deleting cover image for user: {}", userId);

        return deleteMedia(publicId)
                .doOnSuccess(v -> log.info("Cover image deleted for user: {}", userId))
                .doOnError(e -> log.error("Error deleting cover image for user {}: {}", userId, e.getMessage()));
    }
}