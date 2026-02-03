package io.github.lvoxx.user_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import io.github.lvoxx.user_service.dto.CreateUserRequest;
import io.github.lvoxx.user_service.dto.UpdateUserRequest;
import io.github.lvoxx.user_service.dto.UserDTO;
import io.github.lvoxx.user_service.dto.UserInterestDTO;
import io.github.lvoxx.user_service.dto.UserPreferencesDTO;
import io.github.lvoxx.user_service.model.User;
import io.github.lvoxx.user_service.model.UserInterest;
import io.github.lvoxx.user_service.model.UserPreferences;

/**
 * MapStruct mapper for User entities and DTOs
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    // User mappings
    UserDTO toDto(User user);

    User toEntity(UserDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isVerified", constant = "false")
    @Mapping(target = "isPrivate", constant = "false")
    @Mapping(target = "followerCount", constant = "0L")
    @Mapping(target = "followingCount", constant = "0L")
    @Mapping(target = "postCount", constant = "0L")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    User toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "followerCount", ignore = true)
    @Mapping(target = "followingCount", ignore = true)
    @Mapping(target = "postCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    // UserPreferences mappings
    UserPreferencesDTO toDto(UserPreferences preferences);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    UserPreferences toEntity(UserPreferencesDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateEntity(UserPreferencesDTO dto, @MappingTarget UserPreferences preferences);

    // UserInterest mappings
    UserInterestDTO toDto(UserInterest interest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    UserInterest toEntity(UserInterestDTO dto);
}
