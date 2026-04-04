package com.paybridge.ops.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEventView(
        UUID id,
        String aggregateType,
        String aggregateId,
        OutboxEventType eventType,
        OutboxEventStatus status,
        int retryCount,
        OffsetDateTime availableAt,
        OffsetDateTime publishedAt,
        String lastError,
        String payloadJson,
        OffsetDateTime createdAt
) {
}
