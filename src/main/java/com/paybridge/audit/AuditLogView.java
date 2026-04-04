package com.paybridge.audit;

import com.paybridge.payment.domain.PaymentProvider;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogView(
        UUID id,
        AuditAction action,
        AuditOutcome outcome,
        String resourceType,
        String resourceId,
        PaymentProvider provider,
        String actorType,
        String correlationId,
        String message,
        String detailJson,
        OffsetDateTime occurredAt
) {
}
