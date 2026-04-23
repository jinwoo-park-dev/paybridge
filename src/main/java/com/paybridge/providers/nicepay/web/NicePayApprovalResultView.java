package com.paybridge.providers.nicepay.web;

import com.paybridge.payment.application.MoneyDisplayFormatter;
import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.providers.nicepay.NicePayApprovalOutcome;
import java.util.UUID;

public record NicePayApprovalResultView(
    UUID paymentId,
    boolean replayed,
    String orderId,
    String amountDisplay,
    String status,
    String tid,
    String authCode,
    String approvedAtDisplay
) {

    public static NicePayApprovalResultView from(PaymentDetailView detail, boolean replayed) {
        return new NicePayApprovalResultView(
            detail.paymentId(),
            replayed,
            detail.orderId(),
            detail.amountDisplay(),
            detail.status().name(),
            dashToNull(detail.providerPaymentId()),
            dashToNull(detail.providerTransactionId()),
            detail.approvedAtDisplay()
        );
    }

    public static NicePayApprovalResultView from(NicePayApprovalOutcome outcome, NicePayKeyInForm form) {
        return new NicePayApprovalResultView(
            outcome.paymentId(),
            outcome.replayed(),
            form.getOrderId(),
            MoneyDisplayFormatter.formatMinor("KRW", form.getAmountMinor()),
            "APPROVED",
            outcome.tid(),
            outcome.authCode(),
            "Just recorded"
        );
    }

    private static String dashToNull(String value) {
        return value == null || value.isBlank() || "-".equals(value) ? null : value;
    }
}
