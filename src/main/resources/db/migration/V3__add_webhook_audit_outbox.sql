create table stripe_webhook_events (
    id uuid primary key,
    provider varchar(20) not null,
    provider_event_id varchar(128) not null,
    event_type varchar(80) not null,
    payload_sha256 varchar(64) not null,
    signature_verified boolean not null,
    processing_status varchar(32) not null,
    correlation_id varchar(100),
    last_error varchar(500),
    processed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_stripe_webhook_provider_event unique (provider, provider_event_id)
);

create index idx_stripe_webhook_status_created_at on stripe_webhook_events (processing_status, created_at);

create table audit_logs (
    id uuid primary key,
    action varchar(48) not null,
    outcome varchar(16) not null,
    resource_type varchar(48) not null,
    resource_id varchar(128),
    provider varchar(20),
    actor_type varchar(32) not null,
    correlation_id varchar(100),
    message varchar(500) not null,
    detail_json text,
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_audit_logs_resource on audit_logs (resource_type, resource_id);
create index idx_audit_logs_occurred_at on audit_logs (occurred_at desc);

create table outbox_events (
    id uuid primary key,
    aggregate_type varchar(48) not null,
    aggregate_id varchar(128) not null,
    event_type varchar(64) not null,
    payload_json text not null,
    status varchar(16) not null,
    retry_count integer not null default 0,
    available_at timestamp with time zone not null,
    published_at timestamp with time zone,
    last_error varchar(500),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_outbox_status_available_at on outbox_events (status, available_at);
create index idx_outbox_aggregate on outbox_events (aggregate_type, aggregate_id);
