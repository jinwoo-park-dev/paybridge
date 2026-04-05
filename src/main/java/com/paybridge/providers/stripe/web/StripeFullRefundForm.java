package com.paybridge.providers.stripe.web;

import jakarta.validation.constraints.NotBlank;

public class StripeFullRefundForm {

    @NotBlank(message = "Refund reason is required.")
    private String refundReason;

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }
}
