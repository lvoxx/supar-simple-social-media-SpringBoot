package io.github.lvoxx.media_service.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import io.github.lvoxx.media_service.model.MediaOriginal;
import reactor.core.publisher.Mono;

public interface MediaOriginalRepository extends R2dbcRepository<MediaOriginal, UUID> {

    Mono<MediaOriginal> findByMediaId(UUID mediaId);
}