package com.paybridge.providers.stripe;

public record StripeCheckoutPageView(
        String paymentIntentId,
        String clientSecret,
        String orderId,
        String amountDisplay,
        String currency,
        String description
) {
}
