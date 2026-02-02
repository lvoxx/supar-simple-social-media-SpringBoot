package io.github.lvoxx.user_service.event;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when user preferences are updated
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPreferencesUpdatedEvent extends UserEvent {
    
    private String username;
    private Map<String, Object> updatedPreferences;
}