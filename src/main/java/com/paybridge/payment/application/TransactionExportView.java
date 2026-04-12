package com.paybridge.payment.application;

import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record TransactionExportView(
        UUID paymentId,
        String orderId,
        PaymentProvider provider,
        PaymentStatus status,
        long amountMinor,
        long reversibleAmountMinor,
        String currency,
        String providerPaymentId,
        String providerTransactionId,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
