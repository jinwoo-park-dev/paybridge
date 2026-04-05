package com.paybridge.providers.nicepay;

public record NicePayKeyInCardDetails(
        String cardNumber,
        String cardExpireYyMm,
        String buyerAuthNumber,
        String cardPasswordTwoDigits
) {
}
