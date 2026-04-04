package com.paybridge.payment.application;

public enum IdempotencyDecision {
    RESERVED,
    REPLAY,
    IN_PROGRESS,
    CONFLICT
}
