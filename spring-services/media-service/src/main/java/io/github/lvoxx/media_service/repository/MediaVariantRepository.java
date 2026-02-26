package io.github.lvoxx.media_service.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import io.github.lvoxx.media_service.model.MediaVariant;
import io.github.lvoxx.media_service.utils.VariantType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaVariantRepository extends R2dbcRepository<MediaVariant, UUID> {

    Flux<MediaVariant> findByMediaId(UUID mediaId);

    @Query("SELECT * FROM media_variant WHERE media_id = :mediaId AND variant = :variant")
    Mono<MediaVariant> findByMediaIdAndVariant(UUID mediaId, VariantType variant);
}