package com.paybridge.providers.stripe;

import java.util.UUID;

public record StripeRefundOutcome(
        UUID paymentId,
        UUID reversalId,
        boolean partial,
        String refundId
) {
}
