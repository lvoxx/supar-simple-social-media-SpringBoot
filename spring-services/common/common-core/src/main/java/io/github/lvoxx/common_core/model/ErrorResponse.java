package io.github.lvoxx.common_core.model;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response for all REST endpoints.
 * 
 * @usage Return in exception handlers
 * @reusable Yes - used by all services
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String error;
    
    private String message;
    
    private Map<String, Object> details;
    
    private String path;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Create error response with error type and message
     */
    public static ErrorResponse of(String error, String message) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .build();
    }
    
    /**
     * Create error response with error type, message and details
     */
    public static ErrorResponse of(String error, String message, Map<String, Object> details) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .details(details)
                .build();
    }
    
    /**
     * Create error response from exception
     */
    public static ErrorResponse fromException(Exception ex) {
        return ErrorResponse.builder()
                .error(ex.getClass().getSimpleName())
                .message(ex.getMessage())
                .build();
    }
}