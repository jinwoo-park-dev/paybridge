package com.paybridge.providers.stripe.webhook;

public record StripeWebhookOutcome(
        String eventId,
        String eventType,
        boolean duplicate,
        StripeWebhookProcessingStatus processingStatus
) {
}
