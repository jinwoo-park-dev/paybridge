package com.paybridge.payment.application;

import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import java.util.UUID;

public record TransactionSummaryView(
        UUID paymentId,
        String orderId,
        PaymentProvider provider,
        PaymentStatus status,
        String amountDisplay,
        String reversibleAmountDisplay,
        String providerPaymentId,
        String providerTransactionId,
        String approvedAtDisplay
) {
}
