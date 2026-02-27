package io.github.lvoxx.elasticsearch_starter.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;

import reactor.core.publisher.Mono;

/**
 * Utility bean for managing Elasticsearch index lifecycle at application
 * startup.
 *
 * <p>
 * Services use this to ensure indexes exist before handling requests.
 * All operations are reactive and non-blocking.
 * </p>
 *
 * <h3>Usage:</h3>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Component
 *     &#64;RequiredArgsConstructor
 *     public class SearchIndexInitializer implements ApplicationRunner {
 *         private final ElasticsearchIndexManager indexManager;
 *
 *         @Override
 *         public void run(ApplicationArguments args) {
 *             indexManager.createIfNotExists("posts")
 *                     .block(); // OK here — only at startup
 *         }
 *     }
 * }
 * </pre>
 */
public class ElasticsearchIndexManager {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexManager.class);

    private final ReactiveElasticsearchClient client;

    public ElasticsearchIndexManager(ReactiveElasticsearchClient client) {
        this.client = client;
    }

    /**
     * Creates an index if it does not already exist.
     *
     * @param indexName the name of the Elasticsearch index
     * @return {@code true} if the index was created, {@code false} if it already
     *         existed
     */
    public Mono<Boolean> createIfNotExists(String indexName) {
        return exists(indexName)
                .flatMap(exists -> {
                    if (exists) {
                        log.debug("[starter-elasticsearch] Index '{}' already exists — skipping create", indexName);
                        return Mono.just(false);
                    }
                    return client.indices()
                            .create(req -> req.index(indexName))
                            .doOnSuccess(r -> log.info("[starter-elasticsearch] Index '{}' created", indexName))
                            .thenReturn(true);
                });
    }

    /**
     * Checks whether an index exists.
     *
     * @param indexName index name
     * @return {@code true} if the index exists
     */
    public Mono<Boolean> exists(String indexName) {
        return client.indices()
                .exists(req -> req.index(indexName))
                .map(r -> r.value())
                .onErrorResume(e -> {
                    log.warn("[starter-elasticsearch] Failed to check index '{}': {}", indexName, e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Deletes an index. Use with caution — primarily for testing and index
     * migrations.
     *
     * @param indexName index name to delete
     */
    public Mono<Void> delete(String indexName) {
        return client.indices()
                .delete(req -> req.index(indexName))
                .doOnSuccess(r -> log.warn("[starter-elasticsearch] Index '{}' deleted", indexName))
                .then();
    }
}
