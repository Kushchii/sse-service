package com.sse.service.service;

import com.sse.service.api.request.TransactionsRequest;
import com.sse.service.api.response.TransactionsResponse;
import com.sse.service.mapper.TransactionMapper;
import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import com.sse.service.persistent.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

    private final Sinks.Many<TransactionsEntity> newTransactionsSink = Sinks.many().multicast().directBestEffort();
    private final Sinks.Many<TransactionsEntity> allTransactionsSink = Sinks.many().replay().all();

    @Override
    public Mono<TransactionsResponse> transactions(TransactionsRequest request) {
        return processTransactionAndSave(request)
                .flatMap(this::processTransaction)
                .doOnSuccess(it -> log.info("Transaction processed successfully: {}", request.getId()))
                .onErrorResume(e -> handleTransactionError(request, e).then())
                .thenReturn(new TransactionsResponse("Transaction processed successfully"));
    }

    @Override
    public Flux<TransactionsEntity> streamAllTransactions() {
        return allTransactionsSink.asFlux()
                .doOnNext(tx -> log.info("Streaming all transactions: {}", tx.getTransactionId()))
                .doOnTerminate(() -> log.info("Stream all transactions finished"));
    }

    @Override
    public Flux<TransactionsEntity> streamNewTransactions() {
        Instant subscriptionTime = Instant.now();
        return newTransactionsSink.asFlux()
                .filter(tx -> tx.getCreatedAt().toInstant(ZoneOffset.UTC).isAfter(subscriptionTime))
                .doOnSubscribe(s -> log.info("Subscribed to new transactions stream at {}", subscriptionTime))
                .doOnNext(tx -> log.info("New transaction streamed: {}", tx.getTransactionId()))
                .doOnCancel(() -> log.info("Stream new transactions cancelled"))
                .doOnTerminate(() -> log.info("Stream new transactions finished"));
    }

    private Mono<TransactionsEntity> processTransactionAndSave(TransactionsRequest request) {
        TransactionsEntity entity = transactionMapper.toEntity(request);
        return transactionRepository.save(entity)
                .doOnSuccess(savedEntity -> {
                    log.info("Transaction entity saved: {}", savedEntity.getTransactionId());
                    publishTransaction(savedEntity);
                });
    }

    // Метод для додавання транзакції до потоку
    public void publishTransaction(TransactionsEntity transaction) {
        Sinks.EmitResult result = newTransactionsSink.tryEmitNext(transaction);
        log.info("Emitted to sink: {}, result: {}", transaction.getTransactionId(), result);
    }

    private Mono<Void> processTransaction(TransactionsEntity entity) {
        return Mono.fromRunnable(() -> log.info("Transaction processed with id: {}", entity.getTransactionId()));
    }

    public Mono<TransactionsResponse> handleTransactionError(TransactionsRequest request, Throwable error) {
        log.error("Transaction processing failed: {}", request.getId(), error);
        return Mono.just(new TransactionsResponse("Transaction failed due to an unexpected error"));
    }
}