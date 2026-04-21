package com.paybridge.providers.stripe;

import com.paybridge.payment.application.MoneyDisplayFormatter;
import java.util.Set;

public record StripeCheckoutPageView(
        String paymentIntentId,
        String clientSecret,
        String orderId,
        String amountDisplay,
        String currency,
        String description,
        String paymentIntentStatus,
        boolean confirmationRequired
) {

    private static final Set<String> CONFIRMABLE_STATUSES = Set.of(
            "requires_payment_method",
            "requires_confirmation",
            "requires_action"
    );

    public static StripeCheckoutPageView from(StripePaymentIntentResponse response, String fallbackOrderId, String fallbackDescription) {
        String status = response.status() == null ? "unknown" : response.status().trim().toLowerCase();
        return new StripeCheckoutPageView(
                response.id(),
                response.clientSecret(),
                firstPresent(response.orderId(), fallbackOrderId),
                MoneyDisplayFormatter.formatMinor(response.currency(), response.amountMinor()),
                response.currency(),
                firstPresent(response.description(), fallbackDescription),
                status,
                response.clientSecret() != null && CONFIRMABLE_STATUSES.contains(status)
        );
    }

    public boolean alreadySucceeded() {
        return "succeeded".equalsIgnoreCase(paymentIntentStatus);
    }

    private static String firstPresent(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }
}
