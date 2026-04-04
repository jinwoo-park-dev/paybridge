package com.paybridge.payment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentReversal(
        UUID reversalId,
        UUID paymentId,
        ReversalType reversalType,
        ReversalStatus reversalStatus,
        long amountMinor,
        long remainingAmountMinor,
        String reason,
        String providerReversalId,
        Instant processedAt
) {

    public PaymentReversal {
        Objects.requireNonNull(reversalId, "reversalId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(reversalType, "reversalType must not be null");
        Objects.requireNonNull(reversalStatus, "reversalStatus must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");

        if (amountMinor <= 0) {
            throw new PaymentDomainException("reversal amount must be greater than zero");
        }
        if (remainingAmountMinor < 0) {
            throw new PaymentDomainException("remainingAmountMinor must not be negative");
        }
        reason = normalizeText(reason, "reason");
        providerReversalId = normalizeOptionalText(providerReversalId);
    }

    public static PaymentReversal succeeded(
            UUID reversalId,
            UUID paymentId,
            ReversalType reversalType,
            long amountMinor,
            long remainingAmountMinor,
            String reason,
            String providerReversalId,
            Instant processedAt
    ) {
        return new PaymentReversal(
                reversalId,
                paymentId,
                reversalType,
                ReversalStatus.SUCCEEDED,
                amountMinor,
                remainingAmountMinor,
                reason,
                providerReversalId,
                processedAt
        );
    }

    private static String normalizeText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PaymentDomainException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
