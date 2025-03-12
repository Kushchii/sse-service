package com.sse.service.config;

import com.sse.service.handler.TransactionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@EnableWebFlux
@Configuration
@RequiredArgsConstructor
public class WebFluxConfiguration implements WebFluxConfigurer {

    public static final String TRANSACTIONS = "/api/transactions";
    public static final String STREAM_ALL_TRANSACTIONS = "/api/transactions/stream/all";
    public static final String STREAM_NEW_TRANSACTIONS = "/api/transactions/stream/new";

    @Bean
    public RouterFunction<ServerResponse> singleStepPaymentRouterFunction(
            TransactionHandler handler) {
        return route()
                .POST(TRANSACTIONS, handler::transactions)
                .GET(STREAM_ALL_TRANSACTIONS, handler::streamAllTransactions)
                .GET(STREAM_NEW_TRANSACTIONS, handler::streamNewTransactions)
                .build();
    }
}
