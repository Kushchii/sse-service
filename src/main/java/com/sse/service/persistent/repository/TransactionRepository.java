package com.sse.service.persistent.repository;

import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.util.UUID;


public interface TransactionRepository extends R2dbcRepository<TransactionsEntity, Long> {

    Mono<TransactionsEntity> findByTransactionId(UUID id);

    @Query("SELECT * FROM transactions WHERE created_at >= :since ORDER BY created_at ASC")
    Flux<TransactionsEntity> findTransactionsSince(Timestamp since);

}

