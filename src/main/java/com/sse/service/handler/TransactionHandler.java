package com.sse.service.handler;


import com.sse.service.api.request.TransactionsRequest;
import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import com.sse.service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Validated
@RequiredArgsConstructor
public class TransactionHandler extends BaseHandler {

    private final TransactionService transactionService;

    public Mono<ServerResponse> transactions(ServerRequest request) {
        log.info("Transaction request received ");
        return request.bodyToMono(TransactionsRequest.class)
                .flatMap(transactionService::transactions)
                .flatMap(it -> toServerResponse(HttpStatus.OK, it));
    }

    public Mono<ServerResponse> streamAllTransactions(ServerRequest request) {
        Flux<TransactionsEntity> transactionStream = transactionService.streamAllTransactions();

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(transactionStream, TransactionsEntity.class);
    }

    public Mono<ServerResponse> streamNewTransactions(ServerRequest request) {
        Flux<TransactionsEntity> transactionStream = transactionService.streamNewTransactions();

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(transactionStream, TransactionsEntity.class);
    }
}
