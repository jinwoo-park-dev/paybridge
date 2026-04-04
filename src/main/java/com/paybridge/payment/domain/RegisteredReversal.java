package com.paybridge.payment.domain;

public record RegisteredReversal(
        Payment updatedPayment,
        PaymentReversal reversal
) {
}
