package io.github.lvoxx.user_service.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all user events
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserCreatedEvent.class, name = "USER_CREATED"),
    @JsonSubTypes.Type(value = UserUpdatedEvent.class, name = "USER_UPDATED"),
    @JsonSubTypes.Type(value = UserDeletedEvent.class, name = "USER_DELETED"),
    @JsonSubTypes.Type(value = UserPreferencesUpdatedEvent.class, name = "USER_PREFERENCES_UPDATED")
})
public abstract class UserEvent {
    
    private String eventId;
    private String eventType;
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String source;
}