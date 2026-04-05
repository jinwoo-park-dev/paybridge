package com.paybridge.providers.stripe.webhook;

public enum StripeWebhookProcessingStatus {
    RECEIVED,
    PROCESSED,
    DUPLICATE,
    REJECTED,
    FAILED
}
