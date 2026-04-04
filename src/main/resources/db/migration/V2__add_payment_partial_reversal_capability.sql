alter table payments
    add column partial_reversal_supported boolean not null default true;
