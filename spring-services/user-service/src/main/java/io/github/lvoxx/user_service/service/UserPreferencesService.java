package io.github.lvoxx.user_service.service;

import io.github.lvoxx.user_service.dto.UserPreferencesDTO;
import reactor.core.publisher.Mono;

/**
 * Service interface for user preferences operations
 */
public interface UserPreferencesService {
    
    /**
     * Get user preferences
     */
    Mono<UserPreferencesDTO> getUserPreferences(Long userId);
    
    /**
     * Update user preferences
     */
    Mono<UserPreferencesDTO> updateUserPreferences(Long userId, UserPreferencesDTO preferencesDTO);
    
    /**
     * Create default preferences for user
     */
    Mono<UserPreferencesDTO> createDefaultPreferences(Long userId);
}
