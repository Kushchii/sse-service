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
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;

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
                .doOnTerminate(() -> log.info("Stream all transactions finished"))
                .doFinally(signal -> log.info("Stream all transactions finalized with signal: {}", signal));
    }

    @Override
    public Flux<TransactionsEntity> streamNewTransactions() {
        var subscriptionTime = Instant.now();
        return newTransactionsSink.asFlux()
                .publishOn(Schedulers.parallel())
                .onBackpressureBuffer(1000)
                .filter(tx -> tx.getCreatedAt().toInstant(ZoneOffset.UTC).isAfter(subscriptionTime))
                .distinct(TransactionsEntity::getTransactionId)
                .doOnDiscard(TransactionsEntity.class, tx -> log.warn("Transaction discarded: {}", tx.getTransactionId()))
                .doOnSubscribe(s -> log.info("Subscribed to new transactions stream at {}", subscriptionTime))
                .doOnNext(tx -> log.info("New transaction streamed: {}", tx.getTransactionId()))
                .doOnCancel(() -> log.info("Stream new transactions cancelled"))
                .doOnTerminate(() -> log.info("Stream new transactions finished"));
    }

    private Mono<TransactionsEntity> processTransactionAndSave(TransactionsRequest request) {
        var entity = transactionMapper.toEntity(request);
        return transactionRepository.save(entity)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnSuccess(savedEntity -> {
                    log.info("Transaction entity saved: {}", savedEntity.getTransactionId());
                    publishTransaction(savedEntity);
                });
    }

    public void publishTransaction(TransactionsEntity transaction) {
        var resultAll = allTransactionsSink.tryEmitNext(transaction);
        var resultNew = newTransactionsSink.tryEmitNext(transaction);
        log.info("Emitted to new transaction sink: {}, result: {}", transaction.getTransactionId(), resultNew);
        log.info("Emitted to all transaction sink: {}, result: {}", transaction.getTransactionId(), resultAll);
    }

    private Mono<Void> processTransaction(TransactionsEntity entity) {
        return Mono.fromRunnable(() -> log.info("Transaction processed with id: {}", entity.getTransactionId()));
    }

    public Mono<TransactionsResponse> handleTransactionError(TransactionsRequest request, Throwable error) {
        log.error("Transaction processing failed: {}", request.getId(), error);
        return Mono.just(new TransactionsResponse("Transaction failed due to an unexpected error"));
    }
}