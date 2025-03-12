package com.sse.service.service;


import com.sse.service.api.request.TransactionsRequest;
import com.sse.service.api.response.TransactionsResponse;
import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {

    Mono<TransactionsResponse> transactions(TransactionsRequest request);

    Flux<TransactionsEntity> streamNewTransactions();

    Flux<TransactionsEntity> streamAllTransactions();
}
