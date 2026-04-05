package com.paybridge.providers.nicepay;

import java.util.UUID;

public record NicePayCancellationOutcome(
        UUID paymentId,
        UUID reversalId,
        boolean partial
) {
}
