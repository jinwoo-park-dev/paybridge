create table payments (
    id uuid primary key,
    order_id varchar(100) not null,
    provider varchar(20) not null,
    status varchar(32) not null,
    amount_minor bigint not null,
    reversible_amount_minor bigint not null,
    currency varchar(3) not null,
    provider_payment_id varchar(128),
    provider_transaction_id varchar(128),
    approved_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_payments_order_id on payments (order_id);
create index idx_payments_provider_payment_id on payments (provider_payment_id);
create index idx_payments_provider_transaction_id on payments (provider_transaction_id);
create index idx_payments_status_created_at on payments (status, created_at);

create table payment_reversals (
    id uuid primary key,
    payment_id uuid not null,
    reversal_type varchar(16) not null,
    status varchar(16) not null,
    amount_minor bigint not null,
    remaining_amount_minor bigint not null,
    reason varchar(255) not null,
    provider_reversal_id varchar(128),
    processed_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payment_reversals_payment_id foreign key (payment_id) references payments(id)
);

create index idx_payment_reversals_payment_id on payment_reversals (payment_id);
create index idx_payment_reversals_type_created_at on payment_reversals (reversal_type, created_at);

create table idempotency_keys (
    id uuid primary key,
    idempotency_key varchar(120) not null,
    operation varchar(32) not null,
    request_hash varchar(128) not null,
    status varchar(16) not null,
    locked_until timestamp with time zone not null,
    result_payment_id uuid,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_idempotency_operation_key unique (operation, idempotency_key)
);

create index idx_idempotency_payment_id on idempotency_keys (result_payment_id);
