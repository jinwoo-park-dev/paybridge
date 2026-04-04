create unique index uq_payments_provider_payment_id
    on payments (provider, provider_payment_id);

create unique index uq_payments_provider_transaction_id
    on payments (provider, provider_transaction_id);

create unique index uq_payment_reversals_payment_provider_reversal_id
    on payment_reversals (payment_id, provider_reversal_id);
