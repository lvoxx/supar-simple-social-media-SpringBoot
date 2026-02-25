package io.github.lvoxx.media_service.properties;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "media")
public class MediaProperties {
    
    private UploadConfig upload;
    private VariantsConfig variants;
    private UrlConfig url;
    
    @Data
    public static class UploadConfig {
        private long maxSizeImage;
        private long maxSizeVideo;
        private long maxSizeAttachment;
        private String allowedImageFormats;
        private String allowedVideoFormats;
        private String allowedAttachmentFormats;
    }
    
    @Data
    public static class VariantsConfig {
        private Map<String, String> image;
        private Map<String, String> video;
    }
    
    @Data
    public static class UrlConfig {
        private int ttl;
        private String signatureSecret;
    }
}

