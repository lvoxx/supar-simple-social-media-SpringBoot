package io.github.lvoxx.media_service.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import io.github.lvoxx.media_service.model.MediaObject;
import io.github.lvoxx.media_service.utils.MediaStatus;
import reactor.core.publisher.Mono;

public interface MediaObjectRepository extends R2dbcRepository<MediaObject, UUID> {

    @Query("SELECT * FROM media_object WHERE id = :id AND deleted_at IS NULL")
    Mono<MediaObject> findByIdAndNotDeleted(UUID id);

    @Query("UPDATE media_object SET status = :status WHERE id = :id")
    Mono<Void> updateStatus(UUID id, MediaStatus status);

    @Query("UPDATE media_object SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    Mono<Void> softDelete(UUID id);
}