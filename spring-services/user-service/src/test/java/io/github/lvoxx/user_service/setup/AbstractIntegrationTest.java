package io.github.lvoxx.user_service.setup;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for every integration test that requires a real PostgreSQL
 * database.
 *
 * <ul>
 * <li>A single {@link PostgreSQLContainer} is shared across the entire
 * test-suite
 * run (static + {@code @Testcontainers}) so container startup cost is paid only
 * once.</li>
 * <li>{@link #registerDynamicProperties} pushes the container's JDBC/R2DBC URL
 * into
 * Spring's {@code DynamicPropertySource} so {@code spring.r2dbc.*} properties
 * are
 * automatically overridden – no manual {@code @TestPropertySource} needed.</li>
 * <li>Active profile is {@code test} → picks up
 * {@code application-test.yml}.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@SuppressWarnings("resource")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(SetupConfig.Postgres.CONTAINER_IMAGE)
            .withDatabaseName(SetupConfig.Postgres.DATABASE_NAME)
            .withUsername(SetupConfig.Postgres.USERNAME)
            .withPassword(SetupConfig.Postgres.PASSWORD);

    /**
     * Dynamically registers r2dbc connection properties that point to the
     * Testcontainer instance. Spring calls this method before the
     * {@code ApplicationContext} is created.
     */
    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432)
                + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }
}