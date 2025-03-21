package com.sse.service.persistent.repository;

import com.sse.service.persistent.postgres.entity.TransactionsEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;


public interface TransactionRepository extends R2dbcRepository<TransactionsEntity, Long> {


}

