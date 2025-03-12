CREATE TABLE transactions
(
    id             bigserial PRIMARY KEY,
    transaction_id uuid           NOT NULL,
    status         VARCHAR(255)   NOT NULL,
    user_id        VARCHAR(255)   NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    currency       VARCHAR(255)   NOT NULL,
    description    VARCHAR(255)   NOT NULL,
    created_at     timestamp      NOT NULL
);
