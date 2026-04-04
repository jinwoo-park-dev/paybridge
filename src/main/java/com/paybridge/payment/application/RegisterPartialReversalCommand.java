package com.paybridge.payment.application;

import java.time.Instant;
import java.util.UUID;

public record RegisterPartialReversalCommand(
        UUID paymentId,
        long amountMinor,
        String reason,
        String providerReversalId,
        Instant processedAt
) {
}
