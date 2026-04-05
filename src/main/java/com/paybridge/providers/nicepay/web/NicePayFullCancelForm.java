package com.paybridge.providers.nicepay.web;

import jakarta.validation.constraints.Size;

public class NicePayFullCancelForm {

    @Size(max = 100, message = "Cancellation reason must fit NicePay's 100 byte limit.")
    private String cancelReason = "Customer requested full cancellation.";

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
