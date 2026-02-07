package io.github.lvoxx.cloudinary_starter.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.cloudinary.Cloudinary;
import com.cloudinary.EagerTransformation;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import io.github.lvoxx.cloudinary_starter.service.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public abstract class MediaUploadServiceImpl implements MediaUploadService {
    
    protected final Cloudinary cloudinary;
    
    @Value("${cloudinary.upload.folder:user-service}")
    protected String uploadFolder;
    
    @Value("${cloudinary.upload.max-file-size:10485760}")
    protected long maxFileSize;
    
    /**
     * Upload media with custom transformation.
     * 
     * @param publicId Cloudinary public ID (path)
     * @param mediaData Base64-encoded data
     * @param resourceType "image" or "video"
     * @param transformation Optional Cloudinary transformation
     * @return Mono with secure URL
     */
    public Mono<String> uploadMedia(
            String publicId,
            String mediaData,
            String resourceType,
            Transformation<EagerTransformation> transformation) {
        
        log.debug("Uploading {} to publicId: {}", resourceType, publicId);
        
        return Mono.fromCallable(() -> {
            Map<String, Object> options = ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", resourceType,
                "overwrite", true,
                "folder", uploadFolder
            );
            
            if (transformation != null) {
                options.put("transformation", transformation);
            }
            
            Map<String, Object> uploadResult = cloudinary.uploader().upload(mediaData, options);
            String secureUrl = (String) uploadResult.get("secure_url");
            
            log.info("{} uploaded successfully: {}", resourceType, secureUrl);
            return secureUrl;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("Error uploading {}: {}", resourceType, e.getMessage()));
    }
    
    /**
     * Upload image with transformation.
     */
    public Mono<String> uploadImage(String publicId, String imageData, Transformation<EagerTransformation> transformation) {
        return uploadMedia(publicId, imageData, "image", transformation);
    }
    
    /**
     * Upload video with transformation.
     */
    public Mono<String> uploadVideo(String publicId, String videoData, Transformation<EagerTransformation> transformation) {
        return uploadMedia(publicId, videoData, "video", transformation);
    }
    
    /**
     * Delete media from Cloudinary.
     * 
     * @param publicId Cloudinary public ID
     * @return Mono<Void> when deletion completes
     */
    public Mono<Void> deleteMedia(String publicId) {
        log.debug("Deleting media: {}", publicId);
        
        return Mono.fromCallable(() -> {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then()
        .doOnSuccess(v -> log.info("Media deleted: {}", publicId))
        .doOnError(e -> log.error("Error deleting media {}: {}", publicId, e.getMessage()));
    }
    
    /**
     * Delete video from Cloudinary.
     */
    public Mono<Void> deleteVideo(String publicId) {
        log.debug("Deleting video: {}", publicId);
        
        return Mono.fromCallable(() -> {
            Map<String, Object> options = ObjectUtils.asMap("resource_type", "video");
            cloudinary.uploader().destroy(publicId, options);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then()
        .doOnSuccess(v -> log.info("Video deleted: {}", publicId))
        .doOnError(e -> log.error("Error deleting video {}: {}", publicId, e.getMessage()));
    }
}
