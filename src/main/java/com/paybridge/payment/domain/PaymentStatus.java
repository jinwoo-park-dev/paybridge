package com.paybridge.payment.domain;

public enum PaymentStatus {
    READY(false),
    APPROVED(true),
    PARTIALLY_REVERSED(true),
    FULLY_REVERSED(false),
    FAILED(false);

    private final boolean reversible;

    PaymentStatus(boolean reversible) {
        this.reversible = reversible;
    }

    public boolean isReversible() {
        return reversible;
    }
}
