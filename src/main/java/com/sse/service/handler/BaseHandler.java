package com.sse.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class BaseHandler {

    protected Mono<ServerResponse> toServerResponse(HttpStatus status, Object body) {
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
