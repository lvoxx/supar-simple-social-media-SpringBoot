package io.github.lvoxx.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import io.github.lvoxx.cloudinary_starter.dto.ImageUploadRequest;
import io.github.lvoxx.cloudinary_starter.dto.ImageUploadResponse;
import io.github.lvoxx.cloudinary_starter.service.UserImageUploadService;
import io.github.lvoxx.user_service.dto.UpdateUserRequest;
import io.github.lvoxx.user_service.dto.UserDTO;
import io.github.lvoxx.user_service.dto.UserPreferencesDTO;
import io.github.lvoxx.user_service.service.UserPreferencesService;
import io.github.lvoxx.user_service.service.UserService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for ProfileImageController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileImageController Tests")
class ProfileImageControllerTest {

    @Mock
    private UserImageUploadService userImageUploadService;

    @Mock
    private UserService userService;

    @Mock
    private UserPreferencesService userPreferencesService;

    @InjectMocks
    private ProfileImageController controller;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_IMAGE_URL = "https://res.cloudinary.com/demo/image/upload/v123/user-service/avatars/1.jpg";
    private static final String DEFAULT_AVATAR_URL = "https://res.cloudinary.com/demo/image/upload/default-avatar.jpg";
    private static final String TEST_BASE64_IMAGE = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD...";

    private UserDTO mockUserDTO;
    private UserPreferencesDTO mockPreferencesDTO;

    @BeforeEach
    void setUp() {
        mockUserDTO = UserDTO.builder()
                .id(TEST_USER_ID)
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .avatarUrl(TEST_IMAGE_URL)
                .build();

        mockPreferencesDTO = UserPreferencesDTO.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .avatarUrl(TEST_IMAGE_URL)
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
                .build();
    }

    // ========================================================================
    // UPLOAD AVATAR TESTS
    // ========================================================================

    @Test
    @DisplayName("Should successfully upload avatar and update both tables")
    void shouldUploadAvatar() {
        // Given
        ImageUploadRequest request = ImageUploadRequest.builder()
                .imageData(TEST_BASE64_IMAGE)
                .build();

        when(userImageUploadService.uploadAvatar(TEST_USER_ID, TEST_BASE64_IMAGE))
                .thenReturn(Mono.just(TEST_IMAGE_URL));
        when(userService.updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenReturn(Mono.just(mockUserDTO));
        when(userPreferencesService.updateAvatarUrl(TEST_USER_ID, TEST_IMAGE_URL))
                .thenReturn(Mono.just(mockPreferencesDTO));

        // When
        Mono<ResponseEntity<ImageUploadResponse>> result = controller.uploadAvatar(TEST_USER_ID, request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(TEST_IMAGE_URL, response.getBody().getImageUrl());
                    assertEquals("user-service/avatars/" + TEST_USER_ID, response.getBody().getPublicId());
                    assertEquals("avatar", response.getBody().getImageType());
                })
                .verifyComplete();

        // Verify all services called
        verify(userImageUploadService).uploadAvatar(TEST_USER_ID, TEST_BASE64_IMAGE);
        verify(userService).updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class));
        verify(userPreferencesService).updateAvatarUrl(TEST_USER_ID, TEST_IMAGE_URL);
    }

    @Test
    @DisplayName("Should handle Cloudinary upload failure")
    void shouldHandleCloudinaryUploadFailure() {
        // Given
        ImageUploadRequest request = ImageUploadRequest.builder()
                .imageData(TEST_BASE64_IMAGE)
                .build();

        when(userImageUploadService.uploadAvatar(TEST_USER_ID, TEST_BASE64_IMAGE))
                .thenReturn(Mono.error(new RuntimeException("Cloudinary upload failed")));

        // When
        Mono<ResponseEntity<ImageUploadResponse>> result = controller.uploadAvatar(TEST_USER_ID, request);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("Cloudinary upload failed")
                .verify();

        // Verify no database updates attempted
        verify(userService, never()).updateUser(anyLong(), any());
        verify(userPreferencesService, never()).updateAvatarUrl(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should handle user service update failure")
    void shouldHandleUserServiceUpdateFailure() {
        // Given
        ImageUploadRequest request = ImageUploadRequest.builder()
                .imageData(TEST_BASE64_IMAGE)
                .build();

        when(userImageUploadService.uploadAvatar(TEST_USER_ID, TEST_BASE64_IMAGE))
                .thenReturn(Mono.just(TEST_IMAGE_URL));
        when(userService.updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("User not found")));

        // When
        Mono<ResponseEntity<ImageUploadResponse>> result = controller.uploadAvatar(TEST_USER_ID, request);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("User not found")
                .verify();

        // Verify preferences not updated if user update fails
        verify(userPreferencesService, never()).updateAvatarUrl(anyLong(), anyString());
    }

    // ========================================================================
    // UPLOAD COVER IMAGE TESTS
    // ========================================================================

    @Test
    @DisplayName("Should successfully upload cover image and update both tables")
    void shouldUploadCoverImage() {
        // Given
        String coverImageUrl = "https://res.cloudinary.com/demo/image/upload/v123/user-service/covers/1.jpg";
        ImageUploadRequest request = ImageUploadRequest.builder()
                .imageData(TEST_BASE64_IMAGE)
                .build();

        UserDTO userWithCover = UserDTO.builder()
                .id(TEST_USER_ID)
                .username("testuser")
                .coverImageUrl(coverImageUrl)
                .build();

        UserPreferencesDTO prefsWithCover = UserPreferencesDTO.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .coverImageUrl(coverImageUrl)
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
                .build();

        when(userImageUploadService.uploadCoverImage(TEST_USER_ID, TEST_BASE64_IMAGE))
                .thenReturn(Mono.just(coverImageUrl));
        when(userService.updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenReturn(Mono.just(userWithCover));
        when(userPreferencesService.updateCoverImageUrl(TEST_USER_ID, coverImageUrl))
                .thenReturn(Mono.just(prefsWithCover));

        // When
        Mono<ResponseEntity<ImageUploadResponse>> result = controller.uploadCoverImage(TEST_USER_ID, request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(coverImageUrl, response.getBody().getImageUrl());
                    assertEquals("user-service/covers/" + TEST_USER_ID, response.getBody().getPublicId());
                    assertEquals("cover", response.getBody().getImageType());
                })
                .verifyComplete();

        verify(userImageUploadService).uploadCoverImage(TEST_USER_ID, TEST_BASE64_IMAGE);
        verify(userService).updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class));
        verify(userPreferencesService).updateCoverImageUrl(TEST_USER_ID, coverImageUrl);
    }

    // ========================================================================
    // DELETE AVATAR TESTS
    // ========================================================================

    @Test
    @DisplayName("Should successfully delete avatar and set default URL")
    void shouldDeleteAvatar() {
        // Given
        UserDTO userWithDefaultAvatar = UserDTO.builder()
                .id(TEST_USER_ID)
                .username("testuser")
                .avatarUrl(DEFAULT_AVATAR_URL)
                .build();

        UserPreferencesDTO prefsWithDefaultAvatar = UserPreferencesDTO.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .avatarUrl(DEFAULT_AVATAR_URL)
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
                .build();

        when(userImageUploadService.deleteAvatar(TEST_USER_ID))
                .thenReturn(Mono.just(DEFAULT_AVATAR_URL));
        when(userService.updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenReturn(Mono.just(userWithDefaultAvatar));
        when(userPreferencesService.updateAvatarUrl(TEST_USER_ID, DEFAULT_AVATAR_URL))
                .thenReturn(Mono.just(prefsWithDefaultAvatar));

        // When
        Mono<ResponseEntity<Void>> result = controller.deleteAvatar(TEST_USER_ID);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCode());
                    assertNull(response.getBody());
                })
                .verifyComplete();

        verify(userImageUploadService).deleteAvatar(TEST_USER_ID);
        verify(userService).updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class));
        verify(userPreferencesService).updateAvatarUrl(TEST_USER_ID, DEFAULT_AVATAR_URL);
    }

    @Test
    @DisplayName("Should handle delete avatar failure from Cloudinary")
    void shouldHandleDeleteAvatarFailure() {
        // Given
        when(userImageUploadService.deleteAvatar(TEST_USER_ID))
                .thenReturn(Mono.error(new RuntimeException("Cloudinary delete failed")));

        // When
        Mono<ResponseEntity<Void>> result = controller.deleteAvatar(TEST_USER_ID);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("Cloudinary delete failed")
                .verify();

        // Verify no database updates attempted
        verify(userService, never()).updateUser(anyLong(), any());
        verify(userPreferencesService, never()).updateAvatarUrl(anyLong(), anyString());
    }

    // ========================================================================
    // DELETE COVER IMAGE TESTS
    // ========================================================================

    @Test
    @DisplayName("Should successfully delete cover image and set to null")
    void shouldDeleteCoverImage() {
        // Given
        UserDTO userWithoutCover = UserDTO.builder()
                .id(TEST_USER_ID)
                .username("testuser")
                .coverImageUrl(null)
                .build();

        UserPreferencesDTO prefsWithoutCover = UserPreferencesDTO.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .coverImageUrl(null)
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
                .build();

        when(userImageUploadService.deleteCoverImage(TEST_USER_ID))
                .thenReturn(Mono.empty());
        when(userService.updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenReturn(Mono.just(userWithoutCover));
        when(userPreferencesService.updateCoverImageUrl(TEST_USER_ID, null))
                .thenReturn(Mono.just(prefsWithoutCover));

        // When
        Mono<ResponseEntity<Void>> result = controller.deleteCoverImage(TEST_USER_ID);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCode());
                    assertNull(response.getBody());
                })
                .verifyComplete();

        verify(userImageUploadService).deleteCoverImage(TEST_USER_ID);
        verify(userService).updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class));
        verify(userPreferencesService).updateCoverImageUrl(TEST_USER_ID, null);
    }

    // ========================================================================
    // INTEGRATION SCENARIO TESTS
    // ========================================================================

    @Test
    @DisplayName("Should handle complete flow: upload -> update -> verify")
    void shouldHandleCompleteUploadFlow() {
        // Scenario: User uploads avatar, system updates both tables, verify consistency

        // Given - Initial state: no avatar
        UserPreferencesDTO initialPrefs = UserPreferencesDTO.builder()
                .userId(TEST_USER_ID)
                .avatarUrl(null)
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
                .build();

        ImageUploadRequest request = ImageUploadRequest.builder()
                .imageData(TEST_BASE64_IMAGE)
                .build();

        when(userImageUploadService.uploadAvatar(TEST_USER_ID, TEST_BASE64_IMAGE))
                .thenReturn(Mono.just(TEST_IMAGE_URL));
        when(userService.updateUser(eq(TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenReturn(Mono.just(mockUserDTO));
        when(userPreferencesService.updateAvatarUrl(TEST_USER_ID, TEST_IMAGE_URL))
                .thenReturn(Mono.just(mockPreferencesDTO));

        // When - Upload avatar
        Mono<ResponseEntity<ImageUploadResponse>> uploadResult = controller.uploadAvatar(TEST_USER_ID, request);

        // Then - Verify upload successful
        StepVerifier.create(uploadResult)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode());
                    assertEquals(TEST_IMAGE_URL, response.getBody().getImageUrl());
                })
                .verifyComplete();

        // Verify both tables were updated
        verify(userService).updateUser(eq(TEST_USER_ID), argThat(req -> TEST_IMAGE_URL.equals(req.getAvatarUrl())));
        verify(userPreferencesService).updateAvatarUrl(TEST_USER_ID, TEST_IMAGE_URL);
    }
}
