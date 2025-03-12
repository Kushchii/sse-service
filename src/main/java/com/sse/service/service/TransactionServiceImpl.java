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
import reactor.util.retry.Retry;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

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
        return transactionRepository.findAll()
                .doOnTerminate(() -> log.info("Stream finished"));
    }

    @Override
    public Flux<TransactionsEntity> streamNewTransactions() {
        return Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> Flux.defer(() -> {
                    Timestamp since = Timestamp.from(Instant.now().minusSeconds(2));
                    return transactionRepository.findTransactionsSince(since);
                }))
                .distinctUntilChanged(TransactionsEntity::getTransactionId)
                .onBackpressureLatest()
                .retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(3)))
                .doOnNext(tx -> log.info("New transaction streamed: {}", tx.getTransactionId()))
                .doOnCancel(() -> log.info("Stream cancelled"))
                .doOnTerminate(() -> log.info("Stream finished"))
                .share();
    }

    private Mono<TransactionsEntity> processTransactionAndSave(TransactionsRequest request) {
        TransactionsEntity entity = transactionMapper.toEntity(request);
        return transactionRepository.save(entity)
                .doOnSuccess(savedEntity -> log.info("Transaction entity saved: {}", savedEntity.getTransactionId()));
    }

    private Mono<Void> processTransaction(TransactionsEntity entity) {
        return Mono.fromRunnable(() -> log.info("Transaction processed with id: {}", entity.getTransactionId()));
    }

    public Mono<TransactionsResponse> handleTransactionError(TransactionsRequest request, Throwable error) {
        log.error("Transaction processing failed: {}", request.getId(), error);
        return Mono.just(new TransactionsResponse("Transaction failed due to an unexpected error"));
    }
}
