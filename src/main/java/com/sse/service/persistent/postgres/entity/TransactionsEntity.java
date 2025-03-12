package com.sse.service.persistent.postgres.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("transactions")
public class TransactionsEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("transaction_id")
    private UUID transactionId;

    @Column("status")
    private String status;

    @Column("user_id")
    private String userId;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("description")
    private String description;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}