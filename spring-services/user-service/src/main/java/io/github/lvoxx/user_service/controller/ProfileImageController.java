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
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/images")
@RequiredArgsConstructor
@Tag(name = "Profile Images", description = "Upload and manage avatar/cover images")
public class ProfileImageController {
    
    private final UserImageUploadService userImageUploadService;
    private final UserService userService;
    
    @PostMapping(value = "/avatar", consumes = MediaType.APPLICATION_JSON_VALUE, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Upload avatar image",
        description = """
            Upload a new avatar image for the user.
            
            The image will be automatically:
            - Resized to 400x400px
            - Cropped with face detection
            - Optimized for web delivery
            - Converted to best format (WebP/JPEG)
            
            Accepted formats: JPEG, PNG, WebP, GIF
            Max file size: 10MB
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Avatar uploaded successfully",
            content = @Content(schema = @Schema(implementation = ImageUploadResponse.class))
        ),
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
                // Update user's avatarUrl in database
                UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                    .avatarUrl(imageUrl)
                    .build();
                
                return userService.updateUser(userId, updateRequest)
                    .map(user -> {
                        ImageUploadResponse response = ImageUploadResponse.builder()
                            .imageUrl(imageUrl)
                            .publicId("user-service/avatars/" + userId)
                            .imageType("avatar")
                            .build();
                        
                        return ResponseEntity.ok(response);
                    });
            });
    }
    
    @PostMapping(value = "/cover", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Upload cover image",
        description = """
            Upload a new cover/background image for the user.
            
            The image will be automatically:
            - Resized to 1500x500px
            - Cropped from center
            - Optimized for web delivery
            - Converted to best format (WebP/JPEG)
            
            Accepted formats: JPEG, PNG, WebP, GIF
            Max file size: 10MB
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cover image uploaded successfully",
            content = @Content(schema = @Schema(implementation = ImageUploadResponse.class))
        ),
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
                // Update user's coverImageUrl in database
                UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                    .coverImageUrl(imageUrl)
                    .build();
                
                return userService.updateUser(userId, updateRequest)
                    .map(user -> {
                        ImageUploadResponse response = ImageUploadResponse.builder()
                            .imageUrl(imageUrl)
                            .publicId("user-service/covers/" + userId)
                            .imageType("cover")
                            .build();
                        
                        return ResponseEntity.ok(response);
                    });
            });
    }
    
    @DeleteMapping("/avatar")
    @Operation(
        summary = "Delete avatar image",
        description = "Delete the user's avatar image from Cloudinary and set avatarUrl to null"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Avatar deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<Void>> deleteAvatar(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        log.info("Deleting avatar for user: {}", userId);
        
        String publicId = "user-service/avatars/" + userId;
        
        return userImageUploadService.deleteImage(publicId)
            .then(userService.updateUser(userId, 
                UpdateUserRequest.builder().avatarUrl(null).build()))
            .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
    
    @DeleteMapping("/cover")
    @Operation(
        summary = "Delete cover image",
        description = "Delete the user's cover image from Cloudinary and set coverImageUrl to null"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cover image deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<Void>> deleteCoverImage(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        log.info("Deleting cover image for user: {}", userId);
        
        String publicId = "user-service/covers/" + userId;
        
        return userImageUploadService.deleteImage(publicId)
            .then(userService.updateUser(userId,
                UpdateUserRequest.builder().coverImageUrl(null).build()))
            .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
