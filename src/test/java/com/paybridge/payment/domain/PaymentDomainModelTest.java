package com.paybridge.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentDomainModelTest {

    @Test
    void partialReversalReducesRemainingAmountAndChangesStatus() {
        Payment payment = Payment.approved(
                UUID.randomUUID(),
                "ORD-1001",
                PaymentProvider.NICEPAY,
                "TID-1001",
                "AUTH-1001",
                10_000L,
                true,
                "KRW",
                Instant.parse("2026-03-19T12:00:00Z")
        );

        RegisteredReversal registeredReversal = payment.registerPartialReversal(
                UUID.randomUUID(),
                2_500L,
                "Customer requested partial cancellation",
                "REV-1",
                Instant.parse("2026-03-19T12:05:00Z")
        );

        assertThat(registeredReversal.updatedPayment().status()).isEqualTo(PaymentStatus.PARTIALLY_REVERSED);
        assertThat(registeredReversal.updatedPayment().reversibleAmountMinor()).isEqualTo(7_500L);
        assertThat(registeredReversal.reversal().remainingAmountMinor()).isEqualTo(7_500L);
    }

    @Test
    void fullReversalConsumesRemainingAmount() {
        Payment payment = Payment.approved(
                UUID.randomUUID(),
                "ORD-1002",
                PaymentProvider.STRIPE,
                "pi_123",
                null,
                8_000L,
                true,
                "USD",
                Instant.parse("2026-03-19T12:00:00Z")
        );

        RegisteredReversal registeredReversal = payment.registerFullReversal(
                UUID.randomUUID(),
                "Full reversal requested",
                "re_123",
                Instant.parse("2026-03-19T12:06:00Z")
        );

        assertThat(registeredReversal.updatedPayment().status()).isEqualTo(PaymentStatus.FULLY_REVERSED);
        assertThat(registeredReversal.updatedPayment().reversibleAmountMinor()).isZero();
        assertThat(registeredReversal.reversal().amountMinor()).isEqualTo(8_000L);
    }

    @Test
    void partialReversalRejectsFullRemainingAmount() {
        Payment payment = Payment.approved(
                UUID.randomUUID(),
                "ORD-1003",
                PaymentProvider.NICEPAY,
                "TID-1003",
                null,
                5_000L,
                true,
                "KRW",
                Instant.parse("2026-03-19T12:00:00Z")
        );

        assertThatThrownBy(() -> payment.registerPartialReversal(
                UUID.randomUUID(),
                5_000L,
                "Should use full reversal instead",
                null,
                Instant.parse("2026-03-19T12:10:00Z")
        )).isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("use full reversal");
    }

    @Test
    void partialReversalRespectsProviderCapabilityFlag() {
        Payment payment = Payment.approved(
                UUID.randomUUID(),
                "ORD-1004",
                PaymentProvider.NICEPAY,
                "TID-1004",
                null,
                12_000L,
                false,
                "KRW",
                Instant.parse("2026-03-19T12:00:00Z")
        );

        assertThat(payment.allowsPartialReversal()).isFalse();
        assertThatThrownBy(() -> payment.registerPartialReversal(
                UUID.randomUUID(),
                100L,
                "Partial reversal should not be allowed",
                null,
                Instant.parse("2026-03-19T12:10:00Z")
        )).isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("does not allow partial reversal");
    }
}
