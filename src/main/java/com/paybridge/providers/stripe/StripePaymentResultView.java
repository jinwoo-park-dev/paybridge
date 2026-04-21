package com.paybridge.providers.stripe;

import java.util.UUID;

public record StripePaymentResultView(
        boolean success,
        boolean replayed,
        UUID paymentId,
        String paymentIntentId,
        String paymentStatus,
        String orderId,
        String amountDisplay,
        String currency,
        String latestChargeId,
        String headline,
        String message,
        String troubleshootingDetail
) {

    public static StripePaymentResultView success(StripePaymentConfirmationOutcome outcome) {
        return new StripePaymentResultView(
                true,
                outcome.replayed(),
                outcome.paymentId(),
                outcome.paymentIntentId(),
                outcome.paymentStatus(),
                outcome.orderId(),
                outcome.amountDisplay(),
                outcome.currency(),
                outcome.latestChargeId(),
                outcome.replayed() ? "Stripe payment was already recorded" : "Stripe payment recorded successfully",
                outcome.replayed()
                        ? "PayBridge found an existing transaction for this PaymentIntent and reused it instead of creating a duplicate record."
                        : "PayBridge verified the PaymentIntent with Stripe and recorded it in the shared payment lifecycle.",
                null
        );
    }

    public static StripePaymentResultView failure(String paymentIntentId, String message) {
        return new StripePaymentResultView(
                false,
                false,
                null,
                paymentIntentId,
                null,
                null,
                null,
                null,
                null,
                "Stripe payment could not be recorded",
                "PayBridge could not complete return-page verification for this Stripe payment.",
                message
        );
    }
}
