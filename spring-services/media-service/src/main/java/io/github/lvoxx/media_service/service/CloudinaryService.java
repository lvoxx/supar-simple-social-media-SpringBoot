package io.github.lvoxx.media_service.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import io.github.lvoxx.media_service.properties.MediaProperties;
import io.github.lvoxx.media_service.utils.MediaType;
import io.github.lvoxx.media_service.utils.VariantType;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final MediaProperties mediaProperties;

    @Retry(name = "cloudinary")
    public Mono<Map<String, Object>> generateUploadSignature(UUID mediaId, MediaType mediaType) {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis() / 1000;
            String folder = getFolder(mediaType);
            String publicId = folder + "/" + mediaId.toString();

            Map<String, Object> params = new HashMap<>();
            params.put("timestamp", timestamp);
            params.put("public_id", publicId);
            params.put("folder", folder);

            String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

            Map<String, Object> result = new HashMap<>();
            result.put("signature", signature);
            result.put("timestamp", timestamp);
            result.put("api_key", cloudinary.config.apiKey);
            result.put("cloud_name", cloudinary.config.cloudName);
            result.put("public_id", publicId);
            result.put("folder", folder);

            return result;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Retry(name = "cloudinary")
    public Mono<String> generateVariantUrl(String publicId, MediaType mediaType, VariantType variant) {
        return Mono.fromCallable(() -> {
            Map<String, Object> options = new HashMap<>();
            options.put("secure", true);
            options.put("sign_url", true);

            if (mediaType == MediaType.IMAGE || mediaType == MediaType.AVATAR || mediaType == MediaType.COVER) {
                String dimensions = getImageVariantDimensions(variant);
                if (dimensions != null) {
                    String[] parts = dimensions.split("x");
                    options.put("width", Integer.parseInt(parts[0]));
                    options.put("height", Integer.parseInt(parts[1]));
                    options.put("crop", "fill");
                    options.put("quality", "auto");
                    options.put("fetch_format", "auto");
                }
            } else if (mediaType == MediaType.VIDEO) {
                if (variant == VariantType.STREAM) {
                    options.put("resource_type", "video");
                    options.put("quality", "auto");
                }
            }

            return cloudinary.url().generate(publicId, options);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Retry(name = "cloudinary")
    public Mono<String> generateSignedUrl(String publicId, String resourceType, int ttlSeconds) {
        return Mono.fromCallable(() -> {
            long expiration = System.currentTimeMillis() / 1000 + ttlSeconds;

            Map<String, Object> options = new HashMap<>();
            options.put("secure", true);
            options.put("sign_url", true);
            options.put("resource_type", resourceType);
            options.put("type", "upload");
            return cloudinary.url().signed(true).generate(publicId, options);

        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Retry(name = "cloudinary")
    public Mono<Void> deleteMedia(String publicId, String resourceType) {
        return Mono.fromCallable(() -> {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String getFolder(MediaType mediaType) {
        return switch (mediaType) {
            case IMAGE -> "images";
            case VIDEO -> "videos";
            case ATTACHMENT -> "attachments";
            case CHAT_MEDIA -> "chat";
            case AVATAR -> "avatars";
            case COVER -> "covers";
        };
    }

    private String getImageVariantDimensions(VariantType variant) {
        Map<String, String> imageVariants = mediaProperties.getVariants().getImage();
        if (imageVariants == null) {
            return null;
        }

        return switch (variant) {
            case THUMB -> imageVariants.get("thumb");
            case SMALL -> imageVariants.get("small");
            case MEDIUM -> imageVariants.get("medium");
            case LARGE -> imageVariants.get("large");
            default -> null;
        };
    }
}
