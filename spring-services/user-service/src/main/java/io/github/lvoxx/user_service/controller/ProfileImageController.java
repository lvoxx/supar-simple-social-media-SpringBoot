package io.github.lvoxx.user_service.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.cloudinary_starter.dto.ImageUploadRequest;
import io.github.lvoxx.cloudinary_starter.dto.ImageUploadResponse;
import io.github.lvoxx.cloudinary_starter.service.UserImageUploadService;
import io.github.lvoxx.user_service.dto.UpdateUserRequest;
import io.github.lvoxx.user_service.service.UserPreferencesService;
import io.github.lvoxx.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * REST controller for profile image upload operations.
 * Updates both users table and user_preferences table for consistency.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/images")
@RequiredArgsConstructor
@Tag(name = "Profile Images", description = "Upload and manage avatar/cover images")
public class ProfileImageController {

    private final UserImageUploadService userImageUploadService;
    private final UserService userService;
    private final UserPreferencesService userPreferencesService;

    @PostMapping(value = "/avatar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload avatar image", description = """
            Upload a new avatar image for the user.

            The image will be automatically:
            - Resized to 400x400px
            - Cropped with face detection
            - Optimized for web delivery
            - Converted to best format (WebP/JPEG)

            Updates both users table and user_preferences table.

            Accepted formats: JPEG, PNG, WebP, GIF
            Max file size: 10MB
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully", content = @Content(schema = @Schema(implementation = ImageUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid image data or format"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "413", description = "File size exceeds 10MB limit")
    })
    public Mono<ResponseEntity<ImageUploadResponse>> uploadAvatar(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody ImageUploadRequest request) {

        log.info("Uploading avatar for user: {}", userId);

        return userImageUploadService.uploadAvatar(userId, request.getImageData())
                .flatMap(imageUrl -> {
                    // Update user's avatarUrl in users table
                    UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                            .avatarUrl(imageUrl)
                            .build();

                    return userService.updateUser(userId, updateUserRequest)
                            .flatMap(user ->
                    // Update user_preferences table with new avatar URL
                    userPreferencesService.updateAvatarUrl(userId, imageUrl)
                            .thenReturn(user))
                            .map(user -> {
                                ImageUploadResponse response = ImageUploadResponse.builder()
                                        .imageUrl(imageUrl)
                                        .publicId("user-service/avatars/" + userId)
                                        .imageType("avatar")
                                        .build();

                                log.info("Avatar uploaded successfully for user {}: {}", userId, imageUrl);
                                return ResponseEntity.ok(response);
                            });
                })
                .doOnError(e -> log.error("Error uploading avatar for user {}: {}", userId, e.getMessage()));
    }

    @PostMapping(value = "/cover", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload cover image", description = """
            Upload a new cover/background image for the user.

            The image will be automatically:
            - Resized to 1500x500px
            - Cropped from center
            - Optimized for web delivery
            - Converted to best format (WebP/JPEG)

            Updates both users table and user_preferences table.

            Accepted formats: JPEG, PNG, WebP, GIF
            Max file size: 10MB
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cover image uploaded successfully", content = @Content(schema = @Schema(implementation = ImageUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid image data or format"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "413", description = "File size exceeds 10MB limit")
    })
    public Mono<ResponseEntity<ImageUploadResponse>> uploadCoverImage(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody ImageUploadRequest request) {

        log.info("Uploading cover image for user: {}", userId);

        return userImageUploadService.uploadCoverImage(userId, request.getImageData())
                .flatMap(imageUrl -> {
                    // Update user's coverImageUrl in users table
                    UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                            .coverImageUrl(imageUrl)
                            .build();

                    return userService.updateUser(userId, updateUserRequest)
                            .flatMap(user ->
                    // Update user_preferences table with new cover image URL
                    userPreferencesService.updateCoverImageUrl(userId, imageUrl)
                            .thenReturn(user))
                            .map(user -> {
                                ImageUploadResponse response = ImageUploadResponse.builder()
                                        .imageUrl(imageUrl)
                                        .publicId("user-service/covers/" + userId)
                                        .imageType("cover")
                                        .build();

                                log.info("Cover image uploaded successfully for user {}: {}", userId, imageUrl);
                                return ResponseEntity.ok(response);
                            });
                })
                .doOnError(e -> log.error("Error uploading cover image for user {}: {}", userId, e.getMessage()));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Delete avatar image", description = """
            Delete the user's avatar image from Cloudinary.
            Sets avatarUrl to default avatar URL in both users and user_preferences tables.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Avatar deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<Void>> deleteAvatar(
            @Parameter(description = "User ID") @PathVariable Long userId) {

        log.info("Deleting avatar for user: {}", userId);

        return userImageUploadService.deleteAvatar(userId)
                .flatMap(defaultAvatarUrl -> {
                    // Update users table with default avatar
                    UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                            .avatarUrl(defaultAvatarUrl)
                            .build();

                    return userService.updateUser(userId, updateUserRequest)
                            .flatMap(user ->
                    // Update user_preferences table with default avatar
                    userPreferencesService.updateAvatarUrl(userId, defaultAvatarUrl)
                            .thenReturn(user))
                            .then(Mono.just(ResponseEntity.noContent().<Void>build()));
                })
                .doOnSuccess(v -> log.info("Avatar deleted successfully for user: {}", userId))
                .doOnError(e -> log.error("Error deleting avatar for user {}: {}", userId, e.getMessage()));
    }

    @DeleteMapping("/cover")
    @Operation(summary = "Delete cover image", description = """
            Delete the user's cover image from Cloudinary.
            Sets coverImageUrl to null in both users and user_preferences tables.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cover image deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<Void>> deleteCoverImage(
            @Parameter(description = "User ID") @PathVariable Long userId) {

        log.info("Deleting cover image for user: {}", userId);

        return userImageUploadService.deleteCoverImage(userId)
                .then(Mono.defer(() -> {
                    // Update users table - set to null
                    UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                            .coverImageUrl(null)
                            .build();

                    return userService.updateUser(userId, updateUserRequest)
                            .flatMap(user ->
                    // Update user_preferences table - set to null
                    userPreferencesService.updateCoverImageUrl(userId, null)
                            .thenReturn(user))
                            .then(Mono.just(ResponseEntity.noContent().<Void>build()));
                }))
                .doOnSuccess(v -> log.info("Cover image deleted successfully for user: {}", userId))
                .doOnError(e -> log.error("Error deleting cover image for user {}: {}", userId, e.getMessage()));
    }
}