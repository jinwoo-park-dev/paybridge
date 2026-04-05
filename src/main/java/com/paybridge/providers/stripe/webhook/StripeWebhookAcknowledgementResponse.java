package com.paybridge.providers.stripe.webhook;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StripeWebhookAcknowledgementResponse", description = "Minimal acknowledgement returned after Stripe webhook verification and processing.")
public record StripeWebhookAcknowledgementResponse(
        @Schema(description = "Whether the event was accepted at the webhook boundary.")
        boolean received,
        @Schema(description = "Whether the event was suppressed as a duplicate.")
        boolean duplicate,
        @Schema(description = "Stripe event id.", example = "evt_123")
        String eventId,
        @Schema(description = "Stripe event type.", example = "payment_intent.succeeded")
        String eventType,
        @Schema(description = "Final processing status stored by PayBridge.", example = "PROCESSED")
        String processingStatus
) {
}
