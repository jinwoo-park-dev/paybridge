package com.paybridge.providers.stripe;

public record StripeCreatePaymentIntentCommand(
        String orderId,
        long amountMinor,
        String currency,
        String description,
        String customerEmail
) {
}
