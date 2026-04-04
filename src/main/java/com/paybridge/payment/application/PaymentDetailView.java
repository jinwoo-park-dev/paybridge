package com.paybridge.payment.application;

import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import java.util.List;
import java.util.UUID;

public record PaymentDetailView(
        UUID paymentId,
        String orderId,
        PaymentProvider provider,
        PaymentStatus status,
        String amountDisplay,
        String reversibleAmountDisplay,
        String currency,
        String providerPaymentId,
        String providerTransactionId,
        String approvedAtDisplay,
        boolean fullReversalAllowed,
        boolean partialReversalAllowed,
        List<PaymentReversalView> reversals
) {
}
