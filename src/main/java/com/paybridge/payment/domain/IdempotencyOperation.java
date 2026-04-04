package com.paybridge.payment.domain;

public enum IdempotencyOperation {
    CREATE_PAYMENT,
    FULL_REVERSAL,
    PARTIAL_REVERSAL,
    STRIPE_WEBHOOK
}
