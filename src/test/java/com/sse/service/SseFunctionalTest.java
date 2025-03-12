package com.sse.service;

import com.sse.service.api.request.TransactionsRequest;
import com.sse.service.api.response.TransactionsResponse;
import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static com.sse.service.BaseTest.random;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionStreamTest extends BaseFunctionalTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final UUID TRANSACTION_ID = UUID.fromString("221dc9a8-81e7-4bee-afc8-3cd83aae580d");

    private TransactionsRequest createTransactionRequest() {
        var request = random(TransactionsRequest.class);
        request.setId(TRANSACTION_ID);
        return request;
    }

    @Test
    @DisplayName("Stream all transactions successfully")
    void shouldStreamAllTransactions() {
        var request = createTransactionRequest();

        webTestClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), TransactionsRequest.class)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/transactions/stream/all")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(TransactionsEntity.class)
                .getResponseBody()
                .take(1)
                .doOnNext(event -> assertThat(event).isNotNull())
                .blockLast(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Stream new transactions successfully")
    void shouldStreamNewTransactions() {
        // Створення запиту для нової транзакції
        var request = createTransactionRequest();

        // Підписуємося на стрім для нових транзакцій
        var transactionStream = webTestClient.get()
                .uri("/api/transactions/stream/new")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(TransactionsEntity.class)
                .getResponseBody();

        Mono.delay(Duration.ofMillis(1000)).block();

        // Відправляємо POST запит для створення транзакції
        webTestClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), TransactionsRequest.class)
                .exchange()
                .expectStatus().isOk();

        // Перевіряємо, що нова транзакція з'явилась у стрімі
        StepVerifier.create(transactionStream)
                .expectNextMatches(event -> event != null && event.getTransactionId().equals(TRANSACTION_ID))
                .thenCancel()
                .verify(Duration.ofSeconds(5)); // Додаємо таймаут
    }
}