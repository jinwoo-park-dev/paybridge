package com.paybridge.providers.stripe;

import com.fasterxml.jackson.databind.JsonNode;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import org.springframework.http.HttpStatus;

public record StripeRefundResponse(
        String id,
        String status,
        long amountMinor,
        String currency
) {

    public static StripeRefundResponse from(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe refund response was empty.");
        }
        return new StripeRefundResponse(
                required(root, "id"),
                required(root, "status"),
                requiredLong(root, "amount"),
                required(root, "currency").toUpperCase()
        );
    }

    public boolean isSucceeded() {
        return "succeeded".equalsIgnoreCase(status);
    }

    private static String required(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        String value = node == null || node.isNull() ? null : node.asText(null);
        if (value == null || value.isBlank()) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe refund response missing field: " + fieldName);
        }
        return value.trim();
    }

    private static long requiredLong(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isNumber()) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe refund response missing numeric field: " + fieldName);
        }
        return node.longValue();
    }
}
