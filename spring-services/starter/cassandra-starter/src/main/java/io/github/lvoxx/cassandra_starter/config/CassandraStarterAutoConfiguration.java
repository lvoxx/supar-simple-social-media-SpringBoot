package io.github.lvoxx.cassandra_starter.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.ReactiveSessionFactory;
import org.springframework.data.cassandra.config.EnableReactiveCassandraRepositories;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

/**
 * Auto-configuration for reactive Apache Cassandra.
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>{@link CqlSession} — reactive-capable CQL session (managed by Spring Boot
 * autoconfiguration)</li>
 * <li>{@link ReactiveCassandraTemplate} — reactive template for CRUD and custom
 * queries</li>
 * <li>{@link CassandraConverter} — mapping between Java objects and Cassandra
 * rows</li>
 * </ul>
 *
 * <h3>Schema management:</h3>
 * Set {@code spring.cassandra.schema-action=CREATE_IF_NOT_EXISTS} in your
 * service's
 * {@code application.yaml} (already the default in this starter's defaults).
 * For production, use {@code NONE} and manage schema with custom CQL scripts.
 *
 * <h3>Consistency levels:</h3>
 * Default consistency is {@code LOCAL_QUORUM} for all reads/writes.
 * Override per-statement using the
 * {@link com.datastax.oss.driver.api.core.cql.Statement} API.
 */
@AutoConfiguration(after = CassandraAutoConfiguration.class)
@ConditionalOnClass({ CqlSession.class, ReactiveCassandraTemplate.class })
@EnableReactiveCassandraRepositories
public class CassandraStarterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CassandraStarterAutoConfiguration.class);

    /**
     * Provides a {@link ReactiveCassandraTemplate} if one is not already present.
     * Requires an auto-configured {@link ReactiveSessionFactory} from Spring Boot's
     * {@code CassandraReactiveDataAutoConfiguration}.
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveCassandraTemplate reactiveCassandraTemplate(
            ReactiveSessionFactory reactiveSessionFactory,
            CassandraConverter cassandraConverter) {
        log.info("[starter-cassandra] Registering ReactiveCassandraTemplate");
        return new ReactiveCassandraTemplate(reactiveSessionFactory, cassandraConverter);
    }

    /**
     * Provides a {@link CassandraConverter} using a basic
     * {@link CassandraMappingContext}
     * if none is already configured.
     */
    @Bean
    @ConditionalOnMissingBean
    public CassandraConverter cassandraConverter(
            CqlSession cqlSession,
            CassandraMappingContext mappingContext) {
        return new MappingCassandraConverter(mappingContext);
    }

    /**
     * Provides a {@link CassandraMappingContext} with a
     * {@link SimpleUserTypeResolver}
     * if none is already configured.
     */
    @Bean
    @ConditionalOnMissingBean
    public CassandraMappingContext cassandraMappingContext(CqlSession cqlSession) {
        CassandraMappingContext context = new CassandraMappingContext();
        context.setUserTypeResolver(new SimpleUserTypeResolver(cqlSession));
        return context;
    }

    /**
     * Startup log to confirm the starter has been applied.
     */
    @Bean
    public CassandraStarterMarker cassandraStarterMarker(
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${spring.cassandra.keyspace-name:x_social}") String keyspace) {
        log.info("[starter-cassandra] Activated for service='{}' keyspace='{}'", serviceName, keyspace);
        return new CassandraStarterMarker(serviceName, keyspace);
    }

    public record CassandraStarterMarker(String serviceName, String keyspace) {
    }
}
