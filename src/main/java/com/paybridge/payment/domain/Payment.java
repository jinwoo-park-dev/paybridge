package com.paybridge.payment.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Payment {

    private final UUID paymentId;
    private final String orderId;
    private final PaymentProvider provider;
    private final String providerPaymentId;
    private final String providerTransactionId;
    private final long amountMinor;
    private final long reversibleAmountMinor;
    private final boolean partialReversalSupported;
    private final String currency;
    private final PaymentStatus status;
    private final Instant approvedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Payment(
            UUID paymentId,
            String orderId,
            PaymentProvider provider,
            String providerPaymentId,
            String providerTransactionId,
            long amountMinor,
            long reversibleAmountMinor,
            boolean partialReversalSupported,
            String currency,
            PaymentStatus status,
            Instant approvedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.orderId = requireText(orderId, "orderId");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.providerPaymentId = normalizeOptionalText(providerPaymentId);
        this.providerTransactionId = normalizeOptionalText(providerTransactionId);
        this.amountMinor = positiveAmount(amountMinor, "amountMinor");
        if (reversibleAmountMinor < 0 || reversibleAmountMinor > amountMinor) {
            throw new PaymentDomainException("reversibleAmountMinor must be between 0 and amountMinor");
        }
        this.reversibleAmountMinor = reversibleAmountMinor;
        this.partialReversalSupported = partialReversalSupported;
        this.currency = normalizeCurrency(currency);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.approvedAt = approvedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        if (this.providerPaymentId == null && this.providerTransactionId == null) {
            throw new PaymentDomainException("at least one provider identifier must be present");
        }
    }

    public static Payment approved(
            UUID paymentId,
            String orderId,
            PaymentProvider provider,
            String providerPaymentId,
            String providerTransactionId,
            long amountMinor,
            boolean partialReversalSupported,
            String currency,
            Instant approvedAt
    ) {
        Instant timestamp = approvedAt == null ? Instant.now() : approvedAt;
        return new Payment(
                paymentId,
                orderId,
                provider,
                providerPaymentId,
                providerTransactionId,
                amountMinor,
                amountMinor,
                partialReversalSupported,
                currency,
                PaymentStatus.APPROVED,
                timestamp,
                timestamp,
                timestamp
        );
    }

    public static Payment rehydrate(
            UUID paymentId,
            String orderId,
            PaymentProvider provider,
            String providerPaymentId,
            String providerTransactionId,
            long amountMinor,
            long reversibleAmountMinor,
            boolean partialReversalSupported,
            String currency,
            PaymentStatus status,
            Instant approvedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Payment(
                paymentId,
                orderId,
                provider,
                providerPaymentId,
                providerTransactionId,
                amountMinor,
                reversibleAmountMinor,
                partialReversalSupported,
                currency,
                status,
                approvedAt,
                createdAt,
                updatedAt
        );
    }

    public RegisteredReversal registerFullReversal(
            UUID reversalId,
            String reason,
            String providerReversalId,
            Instant processedAt
    ) {
        assertReversible();
        Instant effectiveProcessedAt = processedAt == null ? Instant.now() : processedAt;
        long reversalAmount = reversibleAmountMinor;

        Payment updatedPayment = copyWith(
                0L,
                PaymentStatus.FULLY_REVERSED,
                effectiveProcessedAt
        );

        PaymentReversal reversal = PaymentReversal.succeeded(
                reversalId,
                paymentId,
                ReversalType.FULL,
                reversalAmount,
                0L,
                reason,
                providerReversalId,
                effectiveProcessedAt
        );
        return new RegisteredReversal(updatedPayment, reversal);
    }

    public RegisteredReversal registerPartialReversal(
            UUID reversalId,
            long partialAmountMinor,
            String reason,
            String providerReversalId,
            Instant processedAt
    ) {
        assertReversible();
        if (!partialReversalSupported) {
            throw new PaymentDomainException("payment provider does not allow partial reversal for this transaction");
        }
        positiveAmount(partialAmountMinor, "partialAmountMinor");
        if (partialAmountMinor >= reversibleAmountMinor) {
            throw new PaymentDomainException(
                    "partial reversal amount must be smaller than the remaining reversible amount; use full reversal for the full remaining balance"
            );
        }

        long remainingAmount = reversibleAmountMinor - partialAmountMinor;
        Instant effectiveProcessedAt = processedAt == null ? Instant.now() : processedAt;
        Payment updatedPayment = copyWith(
                remainingAmount,
                PaymentStatus.PARTIALLY_REVERSED,
                effectiveProcessedAt
        );

        PaymentReversal reversal = PaymentReversal.succeeded(
                reversalId,
                paymentId,
                ReversalType.PARTIAL,
                partialAmountMinor,
                remainingAmount,
                reason,
                providerReversalId,
                effectiveProcessedAt
        );
        return new RegisteredReversal(updatedPayment, reversal);
    }

    public boolean allowsFullReversal() {
        return status.isReversible() && reversibleAmountMinor > 0;
    }

    public boolean allowsPartialReversal() {
        return partialReversalSupported && status.isReversible() && reversibleAmountMinor > 0;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public String orderId() {
        return orderId;
    }

    public PaymentProvider provider() {
        return provider;
    }

    public String providerPaymentId() {
        return providerPaymentId;
    }

    public String providerTransactionId() {
        return providerTransactionId;
    }

    public long amountMinor() {
        return amountMinor;
    }

    public long reversibleAmountMinor() {
        return reversibleAmountMinor;
    }

    public boolean partialReversalSupported() {
        return partialReversalSupported;
    }

    public String currency() {
        return currency;
    }

    public PaymentStatus status() {
        return status;
    }

    public Instant approvedAt() {
        return approvedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private Payment copyWith(long newReversibleAmountMinor, PaymentStatus newStatus, Instant newUpdatedAt) {
        return new Payment(
                paymentId,
                orderId,
                provider,
                providerPaymentId,
                providerTransactionId,
                amountMinor,
                newReversibleAmountMinor,
                partialReversalSupported,
                currency,
                newStatus,
                approvedAt,
                createdAt,
                newUpdatedAt
        );
    }

    private void assertReversible() {
        if (!status.isReversible() || reversibleAmountMinor <= 0) {
            throw new PaymentDomainException("payment is not reversible in its current state");
        }
    }

    private static long positiveAmount(long value, String fieldName) {
        if (value <= 0) {
            throw new PaymentDomainException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
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

    private static String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            throw new PaymentDomainException("currency must not be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new PaymentDomainException("currency must be a 3-letter ISO code");
        }
        return normalized;
    }
}
