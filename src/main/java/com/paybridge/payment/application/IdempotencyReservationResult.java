package com.paybridge.payment.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IdempotencyReservationResult(
        IdempotencyDecision decision,
        UUID idempotencyRecordId,
        UUID resultPaymentId,
        OffsetDateTime lockedUntil
) {
}
