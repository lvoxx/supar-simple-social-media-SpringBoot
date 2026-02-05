package io.github.lvoxx.user_service.event;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for synchronizing user data with Elasticsearch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElasticsearchSyncEvent {
    
    private String eventId;
    private String indexName;
    private String documentId;
    private String operation; // INDEX, UPDATE, DELETE
    private Map<String, Object> document;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String source;
}