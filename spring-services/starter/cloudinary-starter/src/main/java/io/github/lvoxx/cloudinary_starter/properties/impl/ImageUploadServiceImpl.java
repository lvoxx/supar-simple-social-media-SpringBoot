package io.github.lvoxx.cloudinary_starter.properties.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.EagerTransformation;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import io.github.lvoxx.cloudinary_starter.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Cloudinary-based image upload service.
 * 
 * Uploads are done on a bounded elastic scheduler to avoid blocking
 * the reactive event loop (Cloudinary SDK is blocking).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class ImageUploadServiceImpl implements ImageUploadService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.upload.folder:user-service}")
    private String uploadFolder;

    @Value("${cloudinary.upload.max-file-size:10485760}")
    private long maxFileSize;

    @Override
    public Mono<String> uploadAvatar(Long userId, String imageData) {
        log.debug("Uploading avatar for user: {}", userId);

        return Mono.fromCallable(() -> {
            String publicId = uploadFolder + "/avatars/" + userId;

            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", true,
                    "transformation", new Transformation<EagerTransformation>()
                            .width(400).height(400)
                            .crop("fill")
                            .gravity("face")
                            .quality("auto")
                            .fetchFormat("auto"),
                    "folder", uploadFolder + "/avatars");

            Map<String, Object> uploadResult = cloudinary.uploader().upload(imageData, options);
            String secureUrl = (String) uploadResult.get("secure_url");

            log.info("Avatar uploaded successfully for user {}: {}", userId, secureUrl);
            return secureUrl;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Error uploading avatar for user {}: {}",
                        userId, e.getMessage()));
    }

    @Override
    public Mono<String> uploadCoverImage(Long userId, String imageData) {
        log.debug("Uploading cover image for user: {}", userId);

        return Mono.fromCallable(() -> {
            String publicId = uploadFolder + "/covers/" + userId;

            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", true,
                    "transformation", new Transformation<EagerTransformation>()
                            .width(1500).height(500)
                            .crop("fill")
                            .gravity("center")
                            .quality("auto")
                            .fetchFormat("auto"),
                    "folder", uploadFolder + "/covers");

            Map<String, Object> uploadResult = cloudinary.uploader().upload(imageData, options);
            String secureUrl = (String) uploadResult.get("secure_url");

            log.info("Cover image uploaded successfully for user {}: {}", userId, secureUrl);
            return secureUrl;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Error uploading cover image for user {}: {}",
                        userId, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteImage(String publicId) {
        log.debug("Deleting image: {}", publicId);

        return Mono.fromCallable(() -> {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return null;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("Image deleted: {}", publicId))
                .doOnError(e -> log.error("Error deleting image {}: {}", publicId, e.getMessage()));
    }
}