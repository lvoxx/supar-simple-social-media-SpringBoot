package io.github.lvoxx.user_service.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;
import io.github.lvoxx.redis_starter.service.LockService;
import io.github.lvoxx.user_service.dto.UserPreferencesDTO;
import io.github.lvoxx.user_service.event.UserPreferencesUpdatedEvent;
import io.github.lvoxx.user_service.mapper.UserMapper;
import io.github.lvoxx.user_service.model.UserPreferences;
import io.github.lvoxx.user_service.repository.UserPreferencesRepository;
import io.github.lvoxx.user_service.repository.UserRepository;
import io.github.lvoxx.user_service.service.EventPublisherService;
import io.github.lvoxx.user_service.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * UserPreferencesService implementation with declarative caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferencesServiceImpl implements UserPreferencesService {
    
    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final LockService lockService;
    private final EventPublisherService eventPublisher;
    
    private static final String LOCK_KEY_PREFIX = "lock:preferences:";
    
    @Override
    @Cacheable(value = "preferences", key = "#userId")
    public Mono<UserPreferencesDTO> getUserPreferences(Long userId) {
        log.debug("Getting preferences for user: {}", userId);
        
        return preferencesRepository.findByUserId(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException.preferences(userId)))
            .map(userMapper::toDto);
    }
    
    @Override
    @Transactional
    @CachePut(value = "preferences", key = "#userId")
    public Mono<UserPreferencesDTO> updateUserPreferences(Long userId, UserPreferencesDTO preferencesDTO) {
        log.info("Updating preferences for user: {}", userId);
        
        String lockKey = LOCK_KEY_PREFIX + userId;
        
        return lockService.executeWithLock(lockKey, () ->
            userRepository.findById(userId)
                .switchIfEmpty(Mono.error(ResourceNotFoundException.user(userId)))
                .then(preferencesRepository.findByUserId(userId))
                .switchIfEmpty(Mono.error(ResourceNotFoundException.preferences(userId)))
                .flatMap(existing -> {
                    userMapper.updateEntity(preferencesDTO, existing);
                    existing.setUpdatedAt(LocalDateTime.now());
                    
                    return preferencesRepository.save(existing)
                        .map(userMapper::toDto)
                        .flatMap(dto -> publishPreferencesUpdatedEvent(userId, preferencesDTO)
                            .then(Mono.just(dto)));
                })
                .doOnSuccess(dto -> log.info("Preferences updated for user: {}", userId))
                .doOnError(e -> log.error("Error updating preferences: {}", e.getMessage()))
        );
    }
    
    @Override
    @Transactional
    public Mono<UserPreferencesDTO> createDefaultPreferences(Long userId) {
        log.info("Creating default preferences for user: {}", userId);
        
        UserPreferences preferences = UserPreferences.createDefaultPreferences(userId);
        
        return preferencesRepository.save(preferences)
            .map(userMapper::toDto)
            .doOnSuccess(dto -> log.info("Default preferences created for user: {}", userId));
    }
    
    private Mono<Void> publishPreferencesUpdatedEvent(Long userId, UserPreferencesDTO dto) {
        return userRepository.findById(userId)
            .flatMap(user -> {
                Map<String, Object> updatedPreferences = new HashMap<>();
                updatedPreferences.put("theme", dto.getTheme());
                updatedPreferences.put("language", dto.getLanguage());
                updatedPreferences.put("defaultPostVisibility", dto.getDefaultPostVisibility());
                
                UserPreferencesUpdatedEvent event = UserPreferencesUpdatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("USER_PREFERENCES_UPDATED")
                    .userId(userId)
                    .username(user.getUsername())
                    .updatedPreferences(updatedPreferences)
                    .timestamp(LocalDateTime.now())
                    .source("user-service")
                    .build();
                
                return eventPublisher.publishUserEvent(event);
            });
    }
}
