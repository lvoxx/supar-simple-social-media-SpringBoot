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

    /**
     * Update only avatar URL in user preferences
     * 
     * @param userId    User ID
     * @param avatarUrl New avatar URL (can be null for default)
     * @return Updated preferences DTO
     */
    Mono<UserPreferencesDTO> updateAvatarUrl(Long userId, String avatarUrl);

    /**
     * Update only cover image URL in user preferences
     * 
     * @param userId        User ID
     * @param coverImageUrl New cover image URL (can be null)
     * @return Updated preferences DTO
     */
    Mono<UserPreferencesDTO> updateCoverImageUrl(Long userId, String coverImageUrl);
}