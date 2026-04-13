package com.paybridge.payment.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paybridge.payment.domain.ReversalStatus;
import com.paybridge.payment.domain.ReversalType;
import java.util.UUID;

public record PaymentReversalView(
        UUID reversalId,
        ReversalType reversalType,
        @JsonProperty("status") ReversalStatus reversalStatus,
        String amountDisplay,
        String remainingAmountDisplay,
        String reason,
        String providerReversalId,
        String processedAtDisplay
) {
}
