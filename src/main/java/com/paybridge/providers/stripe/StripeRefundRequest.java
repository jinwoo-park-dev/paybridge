package com.paybridge.providers.stripe;

import java.util.LinkedHashMap;
import java.util.Map;

public record StripeRefundRequest(
        String paymentIntentId,
        String chargeId,
        Long amountMinor,
        String businessReason,
        String idempotencyKey
) {

    public Map<String, String> toFormParameters() {
        Map<String, String> params = new LinkedHashMap<>();
        if (chargeId != null && !chargeId.isBlank()) {
            params.put("charge", chargeId.trim());
        } else {
            params.put("payment_intent", paymentIntentId.trim());
        }
        if (amountMinor != null) {
            params.put("amount", Long.toString(amountMinor));
        }
        params.put("reason", "requested_by_customer");
        if (businessReason != null && !businessReason.isBlank()) {
            params.put("metadata[paybridge_reason]", businessReason.trim());
        }
        return params;
    }
}
