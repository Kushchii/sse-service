package com.sse.service;

import com.sse.service.persistent.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SseServiceApplication.class},
        properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext
@AutoConfigureWebTestClient(timeout = "PT10M")
@Tag("FunctionalTest")
public abstract class BaseFunctionalTest extends BaseTest {

    private static final String JDBC_PREFIX = "jdbc";
    private static final String R2DBC_PREFIX = "r2dbc";

    private static final PostgreSQLContainer PSQL_CONTAINER = (PostgreSQLContainer) new PostgreSQLContainer("postgres:latest")
            .withUsername("root")
            .withPassword("password")
            .withDatabaseName("redis_service")
            .withExposedPorts(5432);

    @Autowired
    protected WebTestClient client;

    @Autowired
    protected TransactionRepository transactionRepository;

    @DynamicPropertySource
    protected static void registerMockServer(DynamicPropertyRegistry registry) {
        PSQL_CONTAINER.start();
        registry.add("spring.liquibase.url", PSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.r2dbc.url", () -> PSQL_CONTAINER.getJdbcUrl().replace(JDBC_PREFIX, R2DBC_PREFIX));
        registry.add("spring.r2dbc.username", PSQL_CONTAINER::getUsername);
        registry.add("spring.r2dbc.password", PSQL_CONTAINER::getPassword);
    }

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll().block();
    }

    protected <T, R> R doPost(String uri, T request, Class<R> returnType) {
        return client.post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .returnResult(returnType)
                .getResponseBody()
                .blockFirst();
    }

    protected <R> R doGet(String uri, Class<R> returnType) {
        return client.get()
                .uri(uri)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(returnType)
                .getResponseBody()
                .blockFirst();
    }
}
