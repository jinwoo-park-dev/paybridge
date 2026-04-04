package com.paybridge.web.checkout;

import java.util.List;

public record CheckoutOptionView(
        String providerCode,
        String title,
        String badge,
        String description,
        String actionUrl,
        boolean enabled,
        boolean operatorOnly,
        List<String> bullets
) {
}
