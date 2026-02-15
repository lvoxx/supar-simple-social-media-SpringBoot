package io.github.lvoxx.post_service.elasticsearch.repository;

import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.post_service.elasticsearch.model.PostDocument;
import reactor.core.publisher.Flux;

/**
 * Elasticsearch repository for PostDocument.
 */
@Repository
public interface PostDocumentRepository extends ReactiveElasticsearchRepository<PostDocument, String> {
    Flux<PostDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    Flux<PostDocument> findByContentContainingAndIsDeletedFalseOrderByCreatedAtDesc(String content);
}