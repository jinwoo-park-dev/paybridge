package com.paybridge.providers.nicepay.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class NicePayPartialCancelForm {

    @Min(value = 1, message = "Partial cancel amount must be positive.")
    private long cancelAmountMinor;

    @Size(max = 100, message = "Cancellation reason must fit NicePay's 100 byte limit.")
    private String cancelReason = "Customer requested partial cancellation.";

    public long getCancelAmountMinor() {
        return cancelAmountMinor;
    }

    public void setCancelAmountMinor(long cancelAmountMinor) {
        this.cancelAmountMinor = cancelAmountMinor;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
