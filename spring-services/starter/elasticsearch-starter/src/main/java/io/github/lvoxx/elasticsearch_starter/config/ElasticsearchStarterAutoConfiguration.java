package io.github.lvoxx.elasticsearch_starter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;

import io.github.lvoxx.elasticsearch_starter.index.ElasticsearchIndexManager;

/**
 * Auto-configuration for reactive Elasticsearch.
 * 
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>{@link ReactiveElasticsearchClient} — managed by Spring Boot's own
 * autoconfiguration</li>
 * <li>{@link ElasticsearchIndexManager} — utility for index lifecycle
 * management</li>
 * </ul>
 *
 * <p>
 * Connection settings are configured via standard Spring Boot properties
 * ({@code spring.elasticsearch.*}). See the bundled {@code application.yaml}
 * for defaults.
 * </p>
 */
@AutoConfiguration(after = ElasticsearchClientAutoConfiguration.class)
@ConditionalOnClass(ReactiveElasticsearchClient.class)
@EnableReactiveElasticsearchRepositories
public class ElasticsearchStarterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchStarterAutoConfiguration.class);

    /**
     * Provides the {@link ElasticsearchIndexManager} utility bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchIndexManager elasticsearchIndexManager(ReactiveElasticsearchClient client) {
        log.info("[starter-elasticsearch] Registering ElasticsearchIndexManager");
        return new ElasticsearchIndexManager(client);
    }

    /**
     * Startup log marker.
     */
    @Bean
    public ElasticsearchStarterMarker elasticsearchStarterMarker(
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String uris) {
        log.info("[starter-elasticsearch] Activated for service='{}' uris='{}'", serviceName, uris);
        return new ElasticsearchStarterMarker(serviceName, uris);
    }

    public record ElasticsearchStarterMarker(String serviceName, String uris) {
    }
}
