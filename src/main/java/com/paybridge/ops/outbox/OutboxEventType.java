package com.paybridge.ops.outbox;

public enum OutboxEventType {
    PAYMENT_APPROVED,
    PAYMENT_FULLY_REVERSED,
    PAYMENT_PARTIALLY_REVERSED,
    STRIPE_WEBHOOK_ACKNOWLEDGED
}
