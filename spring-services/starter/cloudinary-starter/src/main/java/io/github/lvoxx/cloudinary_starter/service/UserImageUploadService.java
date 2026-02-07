package io.github.lvoxx.cloudinary_starter.service;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.EagerTransformation;
import com.cloudinary.Transformation;

import reactor.core.publisher.Mono;

/**
 * User-specific image upload service.
 * 
 * Handles avatar and cover image uploads with predefined transformations
 * optimized for user profiles.
 */
@Service
public class UserImageUploadService extends MediaUploadService {
    
    public UserImageUploadService(Cloudinary cloudinary) {
        super(cloudinary);
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
     * @param userId User ID for folder organization
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
     * @param userId User ID for folder organization
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
}