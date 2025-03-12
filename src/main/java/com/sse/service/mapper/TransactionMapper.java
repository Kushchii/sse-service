package com.sse.service.mapper;


import com.sse.service.api.request.TransactionsRequest;
import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionsEntity toEntity(TransactionsRequest request) {
        var transaction = new TransactionsEntity();
        transaction.setTransactionId(request.getId());
        transaction.setAmount(request.getAmount());
        transaction.setUserId(request.getUserId());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(request.getStatus());
        transaction.setDescription(request.getDescription());

        return transaction;
    }
}
