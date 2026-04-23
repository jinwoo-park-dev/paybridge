package com.paybridge.web.checkout;

import java.util.List;

public record CheckoutOptionView(
    String providerCode,
    String title,
    String badge,
    String description,
    String actionUrl,
    String buttonLabel,
    boolean enabled,
    boolean operatorOnly,
    String unavailableMessage,
    List<String> bullets
) {
}
