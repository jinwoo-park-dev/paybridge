package com.paybridge.payment.application;

import java.time.Instant;
import java.util.UUID;

public record RegisterFullReversalCommand(
        UUID paymentId,
        String reason,
        String providerReversalId,
        Instant processedAt
) {
}
