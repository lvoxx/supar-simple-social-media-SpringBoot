package io.github.lvoxx.user_service.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.common_core.exception.model.DuplicateResourceException;
import io.github.lvoxx.common_core.exception.model.ResourceNotFoundException;
import io.github.lvoxx.redis_starter.service.LockService;
import io.github.lvoxx.user_service.dto.CreateUserRequest;
import io.github.lvoxx.user_service.dto.UpdateUserRequest;
import io.github.lvoxx.user_service.dto.UserDTO;
import io.github.lvoxx.user_service.event.UserCreatedEvent;
import io.github.lvoxx.user_service.event.UserDeletedEvent;
import io.github.lvoxx.user_service.event.UserUpdatedEvent;
import io.github.lvoxx.user_service.mapper.UserMapper;
import io.github.lvoxx.user_service.model.User;
import io.github.lvoxx.user_service.model.UserPreferences;
import io.github.lvoxx.user_service.repository.UserPreferencesRepository;
import io.github.lvoxx.user_service.repository.UserRepository;
import io.github.lvoxx.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of UserService with caching, locking, and event publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserMapper userMapper;
    private final LockService lockService;
    private final EventPublisherService eventPublisher;
    
    @Value("${app.cache.user-profile-ttl:3600}")
    private long userProfileCacheTtl;
    
    private static final String CACHE_KEY_PREFIX = "user:";
    private static final String LOCK_KEY_PREFIX = "lock:user:";
    
    @Override
    @Transactional
    public Mono<UserDTO> createUser(CreateUserRequest request) {
        log.info("Creating user with username: {}", request.getUsername());
        
        String lockKey = LOCK_KEY_PREFIX + "create:" + request.getUsername();
        
        return lockService.executeWithLock(lockKey, () -> 
            // Check duplicates
            checkDuplicates(request)
                .then(Mono.defer(() -> {
                    // Create user entity
                    User user = userMapper.toEntity(request);
                    user.setAsNew();
                    
                    return userRepository.save(user)
                        .flatMap(savedUser -> {
                            // Create default preferences
                            UserPreferences preferences = createDefaultPreferences(savedUser.getId());
                            
                            return preferencesRepository.save(preferences)
                                .then(Mono.just(savedUser));
                        })
                        .flatMap(savedUser -> {
                            UserDTO dto = userMapper.toDto(savedUser);
                            
                            // Cache the user
                            return cacheUser(dto)
                                .then(Mono.just(dto));
                        })
                        .flatMap(dto -> {
                            // Publish events
                            return publishUserCreatedEvent(dto)
                                .then(publishElasticsearchSyncEvent(dto, "INDEX"))
                                .then(Mono.just(dto));
                        })
                        .doOnSuccess(dto -> log.info("User created successfully: {}", dto.getId()))
                        .doOnError(e -> log.error("Error creating user: {}", e.getMessage()));
                }))
        );
    }
    
    @Override
    public Mono<UserDTO> getUserById(Long id) {
        log.debug("Getting user by ID: {}", id);
        
        String cacheKey = CACHE_KEY_PREFIX + "id:" + id;
        
        return cacheService.get(cacheKey, UserDTO.class)
            .switchIfEmpty(
                userRepository.findById(id)
                    .switchIfEmpty(Mono.error(ResourceNotFoundException.user(id)))
                    .map(userMapper::toDto)
                    .flatMap(dto -> cacheUser(dto).then(Mono.just(dto)))
            )
            .doOnSuccess(dto -> log.debug("User retrieved: {}", dto.getUsername()));
    }
    
    @Override
    public Mono<UserDTO> getUserByUsername(String username) {
        log.debug("Getting user by username: {}", username);
        
        String cacheKey = CACHE_KEY_PREFIX + "username:" + username;
        
        return cacheService.get(cacheKey, UserDTO.class)
            .switchIfEmpty(
                userRepository.findByUsername(username)
                    .switchIfEmpty(Mono.error(ResourceNotFoundException.userByUsername(username)))
                    .map(userMapper::toDto)
                    .flatMap(dto -> cacheUser(dto).then(Mono.just(dto)))
            );
    }
    
    @Override
    public Mono<UserDTO> getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);
        
        String cacheKey = CACHE_KEY_PREFIX + "email:" + email;
        
        return cacheService.get(cacheKey, UserDTO.class)
            .switchIfEmpty(
                userRepository.findByEmail(email)
                    .switchIfEmpty(Mono.error(ResourceNotFoundException.userByEmail(email)))
                    .map(userMapper::toDto)
                    .flatMap(dto -> cacheUser(dto).then(Mono.just(dto)))
            );
    }
    
    @Override
    public Mono<UserDTO> getUserByKeycloakUserId(String keycloakUserId) {
        log.debug("Getting user by Keycloak user ID: {}", keycloakUserId);
        
        String cacheKey = CACHE_KEY_PREFIX + "keycloak:" + keycloakUserId;
        
        return cacheService.get(cacheKey, UserDTO.class)
            .switchIfEmpty(
                userRepository.findByKeycloakUserId(keycloakUserId)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "User not found with Keycloak ID: " + keycloakUserId)))
                    .map(userMapper::toDto)
                    .flatMap(dto -> cacheUser(dto).then(Mono.just(dto)))
            );
    }
    
    @Override
    @Transactional
    public Mono<UserDTO> updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user: {}", id);
        
        String lockKey = LOCK_KEY_PREFIX + "update:" + id;
        
        return lockService.executeWithLock(lockKey, () ->
            userRepository.findById(id)
                .switchIfEmpty(Mono.error(ResourceNotFoundException.user(id)))
                .flatMap(existingUser -> {
                    // Track changes for event
                    Map<String, Object> previousValues = buildPreviousValuesMap(existingUser);
                    
                    // Update entity
                    userMapper.updateEntity(request, existingUser);
                    existingUser.setUpdatedAt(LocalDateTime.now());
                    
                    return userRepository.save(existingUser)
                        .map(userMapper::toDto)
                        .flatMap(dto -> {
                            // Invalidate cache
                            return evictUserCache(dto)
                                .then(Mono.just(dto));
                        })
                        .flatMap(dto -> {
                            // Publish events
                            Map<String, Object> updatedFields = buildUpdatedFieldsMap(request);
                            return publishUserUpdatedEvent(dto, updatedFields, previousValues)
                                .then(publishElasticsearchSyncEvent(dto, "UPDATE"))
                                .then(Mono.just(dto));
                        });
                })
                .doOnSuccess(dto -> log.info("User updated successfully: {}", dto.getId()))
                .doOnError(e -> log.error("Error updating user: {}", e.getMessage()))
        );
    }
    
    @Override
    @Transactional
    public Mono<Void> deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        
        String lockKey = LOCK_KEY_PREFIX + "delete:" + id;
        
        return lockService.executeWithLock(lockKey, () ->
            userRepository.findById(id)
                .switchIfEmpty(Mono.error(ResourceNotFoundException.user(id)))
                .flatMap(user -> {
                    String username = user.getUsername();
                    
                    // Delete preferences first (cascade)
                    return preferencesRepository.deleteByUserId(id)
                        // Delete user
                        .then(userRepository.deleteById(id))
                        // Evict cache
                        .then(evictUserCache(userMapper.toDto(user)))
                        // Publish events
                        .then(publishUserDeletedEvent(id, username))
                        .then(publishElasticsearchSyncEvent(id, "DELETE"));
                })
                .doOnSuccess(v -> log.info("User deleted successfully: {}", id))
                .doOnError(e -> log.error("Error deleting user: {}", e.getMessage()))
        );
    }
    
    @Override
    public Flux<UserDTO> searchUsers(String query, int limit) {
        log.debug("Searching users with query: {}", query);
        
        return userRepository.searchByUsernameOrDisplayName(query, limit)
            .map(userMapper::toDto)
            .doOnComplete(() -> log.debug("Search completed for query: {}", query));
    }
    
    @Override
    public Flux<UserDTO> getVerifiedUsers() {
        log.debug("Getting verified users");
        
        return userRepository.findByIsVerifiedTrue()
            .map(userMapper::toDto);
    }
    
    @Override
    public Mono<Void> incrementFollowerCount(Long userId) {
        log.debug("Incrementing follower count for user: {}", userId);
        
        return userRepository.incrementFollowerCount(userId)
            .then(evictUserCacheById(userId));
    }
    
    @Override
    public Mono<Void> decrementFollowerCount(Long userId) {
        log.debug("Decrementing follower count for user: {}", userId);
        
        return userRepository.decrementFollowerCount(userId)
            .then(evictUserCacheById(userId));
    }
    
    @Override
    public Mono<Void> incrementFollowingCount(Long userId) {
        log.debug("Incrementing following count for user: {}", userId);
        
        return userRepository.incrementFollowingCount(userId)
            .then(evictUserCacheById(userId));
    }
    
    @Override
    public Mono<Void> decrementFollowingCount(Long userId) {
        log.debug("Decrementing following count for user: {}", userId);
        
        return userRepository.decrementFollowingCount(userId)
            .then(evictUserCacheById(userId));
    }
    
    @Override
    public Mono<Void> incrementPostCount(Long userId) {
        log.debug("Incrementing post count for user: {}", userId);
        
        return userRepository.incrementPostCount(userId)
            .then(evictUserCacheById(userId));
    }
    
    @Override
    public Mono<Void> decrementPostCount(Long userId) {
        log.debug("Decrementing post count for user: {}", userId);
        
        return userRepository.decrementPostCount(userId)
            .then(evictUserCacheById(userId));
    }
    
    // Private helper methods
    
    private Mono<Void> checkDuplicates(CreateUserRequest request) {
        return userRepository.existsByUsername(request.getUsername())
            .flatMap(exists -> exists 
                ? Mono.error(DuplicateResourceException.username(request.getUsername()))
                : Mono.empty())
            .then(userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> exists
                    ? Mono.error(DuplicateResourceException.email(request.getEmail()))
                    : Mono.empty()))
            .then(userRepository.existsByKeycloakUserId(request.getKeycloakUserId())
                .flatMap(exists -> exists
                    ? Mono.error(DuplicateResourceException.keycloakUserId(request.getKeycloakUserId()))
                    : Mono.empty()));
    }
    
    private UserPreferences createDefaultPreferences(Long userId) {
        return UserPreferences.builder()
            .userId(userId)
            .showEmail(false)
            .showBirthDate(false)
            .allowTagging(true)
            .allowMentions(true)
            .notifyNewFollower(true)
            .notifyPostLike(true)
            .notifyComment(true)
            .notifyMention(true)
            .notifyMessage(true)
            .defaultPostVisibility("PUBLIC")
            .language("en")
            .timezone("UTC")
            .theme("LIGHT")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
            .setAsNew();
    }
    
    private Mono<Void> cacheUser(UserDTO dto) {
        return Mono.when(
            cacheService.set(CACHE_KEY_PREFIX + "id:" + dto.getId(), dto, userProfileCacheTtl),
            cacheService.set(CACHE_KEY_PREFIX + "username:" + dto.getUsername(), dto, userProfileCacheTtl),
            cacheService.set(CACHE_KEY_PREFIX + "email:" + dto.getEmail(), dto, userProfileCacheTtl),
            cacheService.set(CACHE_KEY_PREFIX + "keycloak:" + dto.getKeycloakUserId(), dto, userProfileCacheTtl)
        );
    }
    
    private Mono<Void> evictUserCache(UserDTO dto) {
        return Mono.when(
            cacheService.delete(CACHE_KEY_PREFIX + "id:" + dto.getId()),
            cacheService.delete(CACHE_KEY_PREFIX + "username:" + dto.getUsername()),
            cacheService.delete(CACHE_KEY_PREFIX + "email:" + dto.getEmail()),
            cacheService.delete(CACHE_KEY_PREFIX + "keycloak:" + dto.getKeycloakUserId())
        );
    }
    
    private Mono<Void> evictUserCacheById(Long userId) {
        return getUserById(userId)
            .flatMap(this::evictUserCache)
            .onErrorResume(e -> Mono.empty()); // Ignore if user not found
    }
    
    private Map<String, Object> buildPreviousValuesMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", user.getDisplayName());
        map.put("bio", user.getBio());
        map.put("location", user.getLocation());
        map.put("website", user.getWebsite());
        map.put("isPrivate", user.getIsPrivate());
        return map;
    }
    
    private Map<String, Object> buildUpdatedFieldsMap(UpdateUserRequest request) {
        Map<String, Object> map = new HashMap<>();
        if (request.getDisplayName() != null) map.put("displayName", request.getDisplayName());
        if (request.getBio() != null) map.put("bio", request.getBio());
        if (request.getLocation() != null) map.put("location", request.getLocation());
        if (request.getWebsite() != null) map.put("website", request.getWebsite());
        if (request.getIsPrivate() != null) map.put("isPrivate", request.getIsPrivate());
        return map;
    }
    
    private Mono<Void> publishUserCreatedEvent(UserDTO dto) {
        UserCreatedEvent event = UserCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("USER_CREATED")
            .userId(dto.getId())
            .username(dto.getUsername())
            .email(dto.getEmail())
            .displayName(dto.getDisplayName())
            .keycloakUserId(dto.getKeycloakUserId())
            .timestamp(LocalDateTime.now())
            .source("user-service")
            .build();
        
        return eventPublisher.publishUserEvent(event);
    }
    
    private Mono<Void> publishUserUpdatedEvent(UserDTO dto, Map<String, Object> updatedFields, 
                                                Map<String, Object> previousValues) {
        UserUpdatedEvent event = UserUpdatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("USER_UPDATED")
            .userId(dto.getId())
            .username(dto.getUsername())
            .updatedFields(updatedFields)
            .previousValues(previousValues)
            .timestamp(LocalDateTime.now())
            .source("user-service")
            .build();
        
        return eventPublisher.publishUserEvent(event);
    }
    
    private Mono<Void> publishUserDeletedEvent(Long userId, String username) {
        UserDeletedEvent event = UserDeletedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("USER_DELETED")
            .userId(userId)
            .username(username)
            .reason("User requested deletion")
            .timestamp(LocalDateTime.now())
            .source("user-service")
            .build();
        
        return eventPublisher.publishUserEvent(event);
    }
    
    private Mono<Void> publishElasticsearchSyncEvent(UserDTO dto, String operation) {
        Map<String, Object> document = new HashMap<>();
        document.put("id", dto.getId());
        document.put("username", dto.getUsername());
        document.put("displayName", dto.getDisplayName());
        document.put("bio", dto.getBio());
        document.put("isVerified", dto.getIsVerified());
        document.put("followerCount", dto.getFollowerCount());
        
        ElasticsearchSyncEvent event = ElasticsearchSyncEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .indexName("users")
            .documentId(String.valueOf(dto.getId()))
            .operation(operation)
            .document(document)
            .timestamp(LocalDateTime.now())
            .source("user-service")
            .build();
        
        return eventPublisher.publishElasticsearchSyncEvent(event);
    }
    
    private Mono<Void> publishElasticsearchSyncEvent(Long userId, String operation) {
        ElasticsearchSyncEvent event = ElasticsearchSyncEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .indexName("users")
            .documentId(String.valueOf(userId))
            .operation(operation)
            .timestamp(LocalDateTime.now())
            .source("user-service")
            .build();
        
        return eventPublisher.publishElasticsearchSyncEvent(event);
    }
}
