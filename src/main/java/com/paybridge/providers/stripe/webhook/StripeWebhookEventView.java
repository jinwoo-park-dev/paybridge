package com.paybridge.providers.stripe.webhook;

import com.paybridge.payment.domain.PaymentProvider;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StripeWebhookEventView(
        UUID id,
        PaymentProvider provider,
        String providerEventId,
        String eventType,
        boolean signatureVerified,
        StripeWebhookProcessingStatus processingStatus,
        String correlationId,
        String lastError,
        OffsetDateTime processedAt,
        OffsetDateTime createdAt
) {
}
