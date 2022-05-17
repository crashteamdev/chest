--liquibase formatted sql
--changeset vitaxa:create-initial-wallet
CREATE TABLE wallet
(
    id      uuid PRIMARY KEY,
    amount  BIGINT NOT NULL,
    user_id CHARACTER VARYING NOT NULL,
    blocked BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX wallet_user_id_idx on wallet (user_id);

CREATE TYPE wallet_history_type as ENUM (
    'replenishment',
    'withdrawal'
);

CREATE TABLE wallet_history
(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    wallet_id uuid NOT NULL,
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount BIGINT NOT NULL,
    description CHARACTER VARYING NOT NULL,
    type wallet_history_type NOT NULL,
    context BYTEA,
    CONSTRAINT fk_wallet FOREIGN KEY(wallet_id) REFERENCES wallet(id)
);

CREATE INDEX wallet_history_wallet_id_idx on wallet (id);

CREATE TABLE wallet_payment
(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    payment_id CHARACTER VARYING NOT NULL,
    wallet_id uuid NOT NULL,
    status CHARACTER VARYING NOT NULL,
    amount BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    description CHARACTER VARYING NOT NULL,
    currency CHARACTER VARYING NOT NULL,

    CONSTRAINT uq_payment_id UNIQUE (payment_id),
    CONSTRAINT fk_wallet FOREIGN KEY(wallet_id) REFERENCES wallet(id)
)
