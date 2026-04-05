package com.paybridge.providers.nicepay;

import java.util.UUID;

public record NicePayApprovalOutcome(
        UUID paymentId,
        boolean replayed,
        String tid,
        String authCode
) {
}
