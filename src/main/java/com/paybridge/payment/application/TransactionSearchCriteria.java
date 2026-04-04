package com.paybridge.payment.application;

import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;

public record TransactionSearchCriteria(
        String orderId,
        String providerPaymentId,
        String providerTransactionId,
        PaymentProvider provider,
        PaymentStatus status
) {

    public TransactionSearchCriteria {
        orderId = normalize(orderId);
        providerPaymentId = normalize(providerPaymentId);
        providerTransactionId = normalize(providerTransactionId);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
