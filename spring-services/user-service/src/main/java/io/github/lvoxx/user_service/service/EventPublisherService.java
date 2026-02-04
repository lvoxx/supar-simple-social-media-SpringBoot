package io.github.lvoxx.user_service.service;

import io.github.lvoxx.user_service.event.UserEvent;
import reactor.core.publisher.Mono;

/**
 * Service interface for publishing events to Kafka
 */
public interface EventPublisherService {
    
    /**
     * Publish user event
     */
    Mono<Void> publishUserEvent(UserEvent event);
    
    /**
     * Publish Elasticsearch sync event
     */
    Mono<Void> publishElasticsearchSyncEvent(ElasticsearchSyncEvent event);
}