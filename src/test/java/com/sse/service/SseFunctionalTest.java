package com.sse.service;

import com.sse.service.api.request.TransactionsRequest;
import com.sse.service.api.response.TransactionsResponse;
import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SpringBootTest
@AutoConfigureWebTestClient
@Slf4j
class SseFunctionalTest extends BaseFunctionalTest {

    private static final UUID TRANSACTION_ID_1 = UUID.fromString("221dc9a8-81e7-4bee-afc8-3cd83aae580d");
    private static final UUID TRANSACTION_ID_2 = UUID.fromString("221dc9a8-81e7-4bee-afc8-3cd83aae580b");

    @Autowired
    private WebTestClient webTestClient;

    private TransactionsRequest createTransactionRequest(UUID uuid) {
        var request = random(TransactionsRequest.class);
        request.setId(uuid);
        System.out.println("Created request: " + request);
        return request;
    }

    @Test
    @DisplayName("Cold Publisher: Stream all transactions from the beginning")
    void shouldStreamAllTransactions() {
        var request1 = createTransactionRequest(TRANSACTION_ID_1);
        webTestClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request1), TransactionsRequest.class)
                .exchange()
                .expectStatus().isOk();

        var request2 = createTransactionRequest(TRANSACTION_ID_2);
        webTestClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request2), TransactionsRequest.class)
                .exchange()
                .expectStatus().isOk();

        var allTransactionsStream = webTestClient.get()
                .uri("/api/transactions/stream/all")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(TransactionsEntity.class)
                .getResponseBody();

        StepVerifier.create(allTransactionsStream)
                .expectNextMatches(tx -> tx.getTransactionId().equals(TRANSACTION_ID_1))
                .expectNextMatches(tx -> tx.getTransactionId().equals(TRANSACTION_ID_2))
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Hot Publisher: Stream only new transactions after subscription")
    void shouldStreamNewTransactions() {
        TransactionsRequest request1 = createTransactionRequest(TRANSACTION_ID_1);
        webTestClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request1), TransactionsRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransactionsResponse.class)
                .consumeWith(response -> System.out.println("First transaction response: " + response.getResponseBody()));

        var streamFuture = CompletableFuture.supplyAsync(() -> webTestClient.get()
                .uri("/api/transactions/stream/new")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(TransactionsEntity.class)
                .getResponseBody()
                .timeout(Duration.ofSeconds(10)));

        var secondTransactionFuture = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                TransactionsRequest request2 = createTransactionRequest(TRANSACTION_ID_2);
                webTestClient.post()
                        .uri("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(request2), TransactionsRequest.class)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(TransactionsResponse.class)
                        .consumeWith(response -> System.out.println("Second transaction response: " + response.getResponseBody()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Second transaction was interrupted", e);
            }
        });

        CompletableFuture.allOf(streamFuture, secondTransactionFuture).join();

        var newTransactionsStream = streamFuture.join();

        StepVerifier.create(newTransactionsStream)
                .expectNextMatches(tx -> {
                    System.out.println("Received in StepVerifier: " + tx);
                    return tx.getTransactionId().equals(TRANSACTION_ID_2);
                })
                .thenCancel()
                .verify(Duration.ofSeconds(10));
    }
}