package com.paybridge.providers.stripe.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class StripePartialRefundForm {

    @Positive(message = "Refund amount must be positive.")
    private long refundAmountMinor;

    @NotBlank(message = "Refund reason is required.")
    private String refundReason;

    public long getRefundAmountMinor() {
        return refundAmountMinor;
    }

    public void setRefundAmountMinor(long refundAmountMinor) {
        this.refundAmountMinor = refundAmountMinor;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }
}
