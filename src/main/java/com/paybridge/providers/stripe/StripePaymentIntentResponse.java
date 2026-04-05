package com.paybridge.providers.stripe;

import com.fasterxml.jackson.databind.JsonNode;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import org.springframework.http.HttpStatus;

public record StripePaymentIntentResponse(
        String id,
        String clientSecret,
        String status,
        long amountMinor,
        String currency,
        String latestChargeId,
        String orderId
) {

    public static StripePaymentIntentResponse from(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe PaymentIntent response was empty.");
        }
        return new StripePaymentIntentResponse(
                required(root, "id"),
                nullableText(root, "client_secret"),
                required(root, "status"),
                requiredLong(root, "amount"),
                required(root, "currency").toUpperCase(),
                nullableText(root, "latest_charge"),
                nullableText(root.path("metadata"), "order_id")
        );
    }

    private static String required(JsonNode root, String fieldName) {
        String value = nullableText(root, fieldName);
        if (value == null || value.isBlank()) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe response missing field: " + fieldName);
        }
        return value;
    }

    private static long requiredLong(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isNumber()) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe response missing numeric field: " + fieldName);
        }
        return node.longValue();
    }

    private static String nullableText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
