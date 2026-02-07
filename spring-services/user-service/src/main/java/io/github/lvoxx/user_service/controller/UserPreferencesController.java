package io.github.lvoxx.user_service.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.user_service.dto.UserPreferencesDTO;
import io.github.lvoxx.user_service.service.UserPreferencesService;
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
 * REST controller for user preferences operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "APIs for managing user preferences and settings")
public class UserPreferencesController {
    
    private final UserPreferencesService preferencesService;
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user preferences", description = "Retrieves preferences for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences found",
            content = @Content(schema = @Schema(implementation = UserPreferencesDTO.class))),
        @ApiResponse(responseCode = "404", description = "User or preferences not found")
    })
    public Mono<ResponseEntity<UserPreferencesDTO>> getUserPreferences(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Getting preferences for user: {}", userId);
        
        return preferencesService.getUserPreferences(userId)
            .map(ResponseEntity::ok);
    }
    
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update user preferences", description = "Updates preferences for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User or preferences not found")
    })
    public Mono<ResponseEntity<UserPreferencesDTO>> updateUserPreferences(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody UserPreferencesDTO preferencesDTO) {
        log.info("Updating preferences for user: {}", userId);
        
        return preferencesService.updateUserPreferences(userId, preferencesDTO)
            .map(ResponseEntity::ok);
    }
}