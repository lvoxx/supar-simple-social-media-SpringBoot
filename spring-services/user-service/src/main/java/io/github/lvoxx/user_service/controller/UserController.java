package io.github.lvoxx.user_service.controller;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.user_service.dto.CreateUserRequest;
import io.github.lvoxx.user_service.dto.UpdateUserRequest;
import io.github.lvoxx.user_service.dto.UserDTO;
import io.github.lvoxx.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for user operations with full HATEOAS support.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "CRUD operations for user profiles")
public class UserController {

    private final UserService userService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new user", description = "Creates a new user profile with default preferences")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "User already exists (duplicate username/email)")
    })
    public Mono<ResponseEntity<EntityModel<UserDTO>>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        log.info("Creating user: {}", request.getUsername());

        return userService.createUser(request)
                .flatMap(this::toEntityModel)
                .map(model -> ResponseEntity.status(HttpStatus.CREATED).body(model));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user by ID", description = "Retrieves a user profile by ID with HATEOAS links")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<EntityModel<UserDTO>>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Getting user by ID: {}", id);

        return userService.getUserById(id)
                .flatMap(this::toEntityModel)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/username/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user by username", description = "Retrieves a user profile by username")
    public Mono<ResponseEntity<EntityModel<UserDTO>>> getUserByUsername(
            @Parameter(description = "Username") @PathVariable String username) {
        log.info("Getting user by username: {}", username);

        return userService.getUserByUsername(username)
                .flatMap(this::toEntityModel)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user by email")
    public Mono<ResponseEntity<EntityModel<UserDTO>>> getUserByEmail(
            @Parameter(description = "Email") @PathVariable String email) {
        log.info("Getting user by email: {}", email);

        return userService.getUserByEmail(email)
                .flatMap(this::toEntityModel)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/keycloak/{keycloakUserId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user by Keycloak ID")
    public Mono<ResponseEntity<EntityModel<UserDTO>>> getUserByKeycloakUserId(
            @Parameter(description = "Keycloak User ID") @PathVariable String keycloakUserId) {
        log.info("Getting user by Keycloak ID: {}", keycloakUserId);

        return userService.getUserByKeycloakUserId(keycloakUserId)
                .flatMap(this::toEntityModel)
                .map(ResponseEntity::ok);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update user profile")
    public Mono<ResponseEntity<EntityModel<UserDTO>>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating user: {}", id);

        return userService.updateUser(id, request)
                .flatMap(this::toEntityModel)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user")
    public Mono<ResponseEntity<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Deleting user: {}", id);

        return userService.deleteUser(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search users", description = "Search users by username or display name with pagination")
    public Flux<EntityModel<UserDTO>> searchUsers(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Result limit") @RequestParam(defaultValue = "20") int limit) {
        log.info("Searching users with query: {}", q);

        return userService.searchUsers(q, limit)
                .flatMap(this::toEntityModel);
    }

    @GetMapping(value = "/verified", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get verified users")
    public Flux<EntityModel<UserDTO>> getVerifiedUsers() {
        log.info("Getting verified users");

        return userService.getVerifiedUsers()
                .flatMap(this::toEntityModel);
    }

    // Counter endpoints
    @PostMapping("/{id}/follower/increment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Increment follower count", description = "Called by Follow Service")
    public Mono<ResponseEntity<Void>> incrementFollowerCount(@PathVariable Long id) {
        return userService.incrementFollowerCount(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PostMapping("/{id}/follower/decrement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decrement follower count")
    public Mono<ResponseEntity<Void>> decrementFollowerCount(@PathVariable Long id) {
        return userService.decrementFollowerCount(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PostMapping("/{id}/following/increment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Increment following count")
    public Mono<ResponseEntity<Void>> incrementFollowingCount(@PathVariable Long id) {
        return userService.incrementFollowingCount(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PostMapping("/{id}/following/decrement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decrement following count")
    public Mono<ResponseEntity<Void>> decrementFollowingCount(@PathVariable Long id) {
        return userService.decrementFollowingCount(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PostMapping("/{id}/posts/increment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Increment post count", description = "Called by Post Service")
    public Mono<ResponseEntity<Void>> incrementPostCount(@PathVariable Long id) {
        return userService.incrementPostCount(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PostMapping("/{id}/posts/decrement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decrement post count")
    public Mono<ResponseEntity<Void>> decrementPostCount(@PathVariable Long id) {
        return userService.decrementPostCount(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    /**
     * Wrap UserDTO in EntityModel with comprehensive HATEOAS links.
     */
    private Mono<EntityModel<UserDTO>> toEntityModel(UserDTO user) {
        return Mono.zip(
                // Self link
                linkTo(methodOn(UserController.class).getUserById(user.getId()))
                        .withSelfRel().toMono(),

                // Update link
                linkTo(methodOn(UserController.class).updateUser(user.getId(), null))
                        .withRel("update").toMono(),

                // Delete link
                linkTo(methodOn(UserController.class).deleteUser(user.getId()))
                        .withRel("delete").toMono(),

                // Preferences link
                linkTo(methodOn(UserPreferencesController.class).getUserPreferences(user.getId()))
                        .withRel("preferences").toMono(),

                // Upload avatar link
                linkTo(methodOn(ProfileImageController.class).uploadAvatar(user.getId(), null))
                        .withRel("uploadAvatar").toMono(),

                // Upload cover link
                linkTo(methodOn(ProfileImageController.class).uploadCoverImage(user.getId(), null))
                        .withRel("uploadCover").toMono(),

                // Search link (template)
                linkTo(methodOn(UserController.class).searchUsers(null, 20))
                        .withRel("search").toMono())
                .map(tuple -> {
                    Link selfLink = tuple.getT1();
                    Link updateLink = tuple.getT2();
                    Link deleteLink = tuple.getT3();
                    Link preferencesLink = tuple.getT4();
                    Link uploadAvatarLink = tuple.getT5();
                    Link uploadCoverLink = tuple.getT6();
                    Link searchLink = tuple.getT7();

                    return EntityModel.of(user,
                            selfLink,
                            updateLink,
                            deleteLink,
                            preferencesLink,
                            uploadAvatarLink,
                            uploadCoverLink,
                            searchLink);
                });
    }
}
