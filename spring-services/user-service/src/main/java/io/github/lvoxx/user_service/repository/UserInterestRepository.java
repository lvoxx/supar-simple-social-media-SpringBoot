package io.github.lvoxx.user_service.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.user_service.model.UserInterest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for UserInterest entity
 */
@Repository
public interface UserInterestRepository extends R2dbcRepository<UserInterest, Long> {

    /**
     * Find all interests for a user
     */
    Flux<UserInterest> findByUserId(Long userId);

    /**
     * Find interests by user and category
     */
    Flux<UserInterest> findByUserIdAndInterestCategory(Long userId, String category);

    /**
     * Find specific interest
     */
    Mono<UserInterest> findByUserIdAndInterestCategoryAndInterestName(
            Long userId, String category, String name);

    /**
     * Delete all interests for a user
     */
    Mono<Void> deleteByUserId(Long userId);

    /**
     * Delete interest by user, category and name
     */
    Mono<Void> deleteByUserIdAndInterestCategoryAndInterestName(
            Long userId, String category, String name);

    /**
     * Check if interest exists
     */
    Mono<Boolean> existsByUserIdAndInterestCategoryAndInterestName(
            Long userId, String category, String name);

    /**
     * Get top interests by weight
     */
    @Query("SELECT * FROM user_interests WHERE user_id = :userId ORDER BY weight DESC LIMIT :limit")
    Flux<UserInterest> findTopInterestsByWeight(Long userId, int limit);
}