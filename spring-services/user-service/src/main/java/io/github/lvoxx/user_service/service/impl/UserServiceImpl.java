package io.github.lvoxx.user_service.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.common_core.exception.model.common_exception.DuplicateResourceException;
import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;
import io.github.lvoxx.common_keys.user_service.UserServiceCacheKeys;
import io.github.lvoxx.common_keys.user_service.UserServiceLockerKeys;
import io.github.lvoxx.error_message_starter.message.CustomMessageResolver;
import io.github.lvoxx.redis_starter.service.LockService;
import io.github.lvoxx.user_service.dto.CreateUserRequest;
import io.github.lvoxx.user_service.dto.UpdateUserRequest;
import io.github.lvoxx.user_service.dto.UserDTO;
import io.github.lvoxx.user_service.event.ElasticsearchSyncEvent;
import io.github.lvoxx.user_service.event.UserCreatedEvent;
import io.github.lvoxx.user_service.event.UserDeletedEvent;
import io.github.lvoxx.user_service.event.UserUpdatedEvent;
import io.github.lvoxx.user_service.mapper.UserMapper;
import io.github.lvoxx.user_service.model.User;
import io.github.lvoxx.user_service.model.UserPreferences;
import io.github.lvoxx.user_service.repository.UserPreferencesRepository;
import io.github.lvoxx.user_service.repository.UserRepository;
import io.github.lvoxx.user_service.service.EventPublisherService;
import io.github.lvoxx.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * UserService implementation with declarative caching via Spring Cache
 * annotations.
 * 
 * Cache strategy:
 * - @Cacheable: Cache the result on read (getUserById, getUserByUsername, etc.)
 * - @CachePut: Update cache after write (updateUser)
 * - @CacheEvict: Invalidate cache on delete or counter updates
 * - Cache name: "users", key: user ID
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
    private final CustomMessageResolver customMessageResolver;

    @Override
    @Transactional
    public Mono<UserDTO> createUser(CreateUserRequest request) {
        log.info("Creating user with username: {}", request.getUsername());

        String lockKey = UserServiceLockerKeys.getUserCreationLockKey(request.getUsername());

        return lockService.executeWithLock(lockKey, () -> checkDuplicates(request)
                .then(Mono.defer(() -> {
                    User user = userMapper.toEntity(request);
                    user.setAsNew();

                    return userRepository.save(user)
                            .flatMap(savedUser -> {
                                UserPreferences preferences = UserPreferences
                                        .createDefaultPreferences(savedUser.getId());

                                return preferencesRepository.save(preferences)
                                        .then(Mono.just(savedUser));
                            })
                            .flatMap(savedUser -> {
                                UserDTO dto = userMapper.toDto(savedUser);

                                return publishUserCreatedEvent(dto)
                                        .then(publishElasticsearchSyncEvent(dto, "INDEX"))
                                        .then(Mono.just(dto));
                            })
                            .doOnSuccess(dto -> log.info("User created successfully: {}", dto.getId()))
                            .doOnError(e -> log.error("Error creating user: {}", e.getMessage()));
                })));
    }

    @Override
    @Cacheable(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.PUT_ID)
    public Mono<UserDTO> getUserById(Long id) {
        log.debug("Getting user by ID: {}", id);

        return userRepository.findById(id)
                .switchIfEmpty(Mono
                        .error(new ResourceNotFoundException(customMessageResolver.get("user.error.id-not-found", id))))
                .map(userMapper::toDto)
                .doOnSuccess(dto -> log.debug("User retrieved: {}", dto.getUsername()));
    }

    @Override
    @Cacheable(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.PUT_USERNAME)
    public Mono<UserDTO> getUserByUsername(String username) {
        log.debug("Getting user by username: {}", username);

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        customMessageResolver.get("user.error.username-not-found", username))))
                .map(userMapper::toDto);
    }

    @Override
    public Mono<UserDTO> getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);

        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException(customMessageResolver.get("user.error.email-not-found", email))))
                .map(userMapper::toDto);
    }

    @Override
    public Mono<UserDTO> getUserByKeycloakUserId(String keycloakUserId) {
        log.debug("Getting user by Keycloak user ID: {}", keycloakUserId);

        return userRepository.findByKeycloakUserId(keycloakUserId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        customMessageResolver.get("user.error.keycloak-user-id-not-found", keycloakUserId))))
                .map(userMapper::toDto);
    }

    @Override
    @Transactional
    @Caching(put = @CachePut(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.PUT_ID), evict = {
            @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_USERNAME),
            @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_EMAIL)
    })
    public Mono<UserDTO> updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user: {}", id);

        String lockKey = UserServiceLockerKeys.getUserUpdateLockKey(id.toString());

        return lockService.executeWithLock(lockKey, () -> userRepository.findById(id)
                .switchIfEmpty(Mono
                        .error(new ResourceNotFoundException(customMessageResolver.get("user.error.id-not-found", id))))
                .flatMap(existingUser -> {
                    Map<String, Object> previousValues = User.buildPreviousValuesMap(existingUser);

                    userMapper.updateEntity(request, existingUser);
                    existingUser.setUpdatedAt(LocalDateTime.now());

                    return userRepository.save(existingUser)
                            .map(userMapper::toDto)
                            .flatMap(dto -> {
                                Map<String, Object> updatedFields = buildUpdatedFieldsMap(request);
                                return publishUserUpdatedEvent(dto, updatedFields, previousValues)
                                        .then(publishElasticsearchSyncEvent(dto, "UPDATE"))
                                        .then(Mono.just(dto));
                            });
                })
                .doOnSuccess(dto -> log.info("User updated successfully: {}", dto.getId()))
                .doOnError(e -> log.error("Error updating user: {}", e.getMessage())));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_ID),
            @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, allEntries = UserServiceCacheKeys.EVICT_ALL) // Clear
                                                                                                               // all
                                                                                                               // user
                                                                                                               // cache
                                                                                                               // on
                                                                                                               // delete
    })
    public Mono<Void> deleteUser(Long id) {
        log.info("Deleting user: {}", id);

        String lockKey = UserServiceLockerKeys.getUserDeletionLockKey(id.toString());

        return lockService.executeWithLock(lockKey, () -> userRepository.findById(id)
                .switchIfEmpty(Mono
                        .error(new ResourceNotFoundException(customMessageResolver.get("user.error.id-not-found", id))))
                .flatMap(user -> {
                    String username = user.getUsername();

                    return preferencesRepository.deleteByUserId(id)
                            .then(userRepository.deleteById(id))
                            .then(publishUserDeletedEvent(id, username))
                            .then(publishElasticsearchSyncEvent(id, "DELETE"));
                })
                .doOnSuccess(v -> log.info("User deleted successfully: {}", id))
                .doOnError(e -> log.error("Error deleting user: {}", e.getMessage())));
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
    @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_ID)
    public Mono<Void> incrementFollowerCount(Long userId) {
        log.debug("Incrementing follower count for user: {}", userId);
        return userRepository.incrementFollowerCount(userId);
    }

    @Override
    @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_ID)
    public Mono<Void> decrementFollowerCount(Long userId) {
        log.debug("Decrementing follower count for user: {}", userId);
        return userRepository.decrementFollowerCount(userId);
    }

    @Override
    @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_ID)
    public Mono<Void> incrementFollowingCount(Long userId) {
        log.debug("Incrementing following count for user: {}", userId);
        return userRepository.incrementFollowingCount(userId);
    }

    @Override
    @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_ID)
    public Mono<Void> decrementFollowingCount(Long userId) {
        log.debug("Decrementing following count for user: {}", userId);
        return userRepository.decrementFollowingCount(userId);
    }

    @Override
    @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_ID)
    public Mono<Void> incrementPostCount(Long userId) {
        log.debug("Incrementing post count for user: {}", userId);
        return userRepository.incrementPostCount(userId);
    }

    @Override
    @CacheEvict(value = UserServiceCacheKeys.USERS_VALUE, key = UserServiceCacheKeys.EVICT_ID)
    public Mono<Void> decrementPostCount(Long userId) {
        log.debug("Decrementing post count for user: {}", userId);
        return userRepository.decrementPostCount(userId);
    }

    // =========================================================================
    // Private helper methods
    // =========================================================================

    private Mono<Void> checkDuplicates(CreateUserRequest request) {
        return userRepository.existsByUsername(request.getUsername())
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateResourceException(
                                customMessageResolver.get("user.error.username.exists", request.getUsername())))
                        : Mono.empty())
                .then(userRepository.existsByEmail(request.getEmail())
                        .flatMap(exists -> exists
                                ? Mono.error(new DuplicateResourceException(
                                        customMessageResolver.get("user.error.email.exists", request.getEmail())))
                                : Mono.empty()))
                .then(userRepository.existsByKeycloakUserId(request.getKeycloakUserId())
                        .flatMap(exists -> exists
                                ? Mono.error(new DuplicateResourceException(customMessageResolver
                                        .get("user.error.keycloak-user-id-not-found", request.getKeycloakUserId())))
                                : Mono.empty()));
    }

    private Map<String, Object> buildUpdatedFieldsMap(UpdateUserRequest request) {
        Map<String, Object> map = new HashMap<>();
        if (request.getDisplayName() != null)
            map.put("displayName", request.getDisplayName());
        if (request.getBio() != null)
            map.put("bio", request.getBio());
        if (request.getLocation() != null)
            map.put("location", request.getLocation());
        if (request.getWebsite() != null)
            map.put("website", request.getWebsite());
        if (request.getIsPrivate() != null)
            map.put("isPrivate", request.getIsPrivate());
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