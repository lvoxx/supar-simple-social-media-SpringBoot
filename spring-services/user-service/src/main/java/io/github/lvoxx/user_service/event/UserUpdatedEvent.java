package io.github.lvoxx.user_service.event;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when user profile is updated
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserUpdatedEvent extends UserEvent {
    
    private String username;
    private Map<String, Object> updatedFields;
    private Map<String, Object> previousValues;
}