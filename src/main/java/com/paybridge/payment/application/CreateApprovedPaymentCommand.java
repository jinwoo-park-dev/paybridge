package com.paybridge.payment.application;

import com.paybridge.payment.domain.PaymentProvider;
import java.time.Instant;

public record CreateApprovedPaymentCommand(
        String orderId,
        PaymentProvider provider,
        String providerPaymentId,
        String providerTransactionId,
        long amountMinor,
        boolean partialReversalSupported,
        String currency,
        Instant approvedAt
) {
}
