package io.github.lvoxx.media_service.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import io.github.lvoxx.media_service.model.MediaAcl;
import reactor.core.publisher.Mono;

public interface MediaAclRepository extends R2dbcRepository<MediaAcl, UUID> {

    @Query("SELECT EXISTS(SELECT 1 FROM media_acl WHERE media_id = :mediaId AND allowed_user_id = :userId)")
    Mono<Boolean> existsByMediaIdAndUserId(UUID mediaId, String userId);
}
