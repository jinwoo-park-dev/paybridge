package com.paybridge.providers.stripe;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record StripeCreatePaymentIntentRequest(
        String orderId,
        long amountMinor,
        String currency,
        String description,
        String customerEmail,
        String idempotencyKey
) {

    public Map<String, String> toFormParameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("amount", Long.toString(amountMinor));
        params.put("currency", currency.trim().toLowerCase(Locale.ROOT));
        params.put("automatic_payment_methods[enabled]", "true");
        params.put("automatic_payment_methods[allow_redirects]", "never");
        params.put("metadata[order_id]", orderId);
        params.put("metadata[source]", "paybridge");
        if (description != null && !description.isBlank()) {
            params.put("description", description.trim());
        }
        if (customerEmail != null && !customerEmail.isBlank()) {
            params.put("receipt_email", customerEmail.trim());
        }
        return params;
    }
}
