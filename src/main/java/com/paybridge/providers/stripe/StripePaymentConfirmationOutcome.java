package com.paybridge.providers.stripe;

import java.util.UUID;

public record StripePaymentConfirmationOutcome(
        UUID paymentId,
        boolean replayed,
        String paymentIntentId,
        String paymentStatus
) {
}
