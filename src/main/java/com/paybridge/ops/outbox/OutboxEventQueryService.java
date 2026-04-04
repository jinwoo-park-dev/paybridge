package com.paybridge.ops.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OutboxEventQueryService {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    public OutboxEventQueryService(OutboxEventJpaRepository outboxEventJpaRepository) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
    }

    public List<OutboxEventView> findByPaymentId(UUID paymentId) {
        return outboxEventJpaRepository.findTop50ByAggregateTypeAndAggregateIdOrderByCreatedAtDesc("payment", paymentId.toString())
                .stream()
                .map(entity -> new OutboxEventView(
                        entity.getId(),
                        entity.getAggregateType(),
                        entity.getAggregateId(),
                        entity.getEventType(),
                        entity.getStatus(),
                        entity.getRetryCount(),
                        entity.getAvailableAt(),
                        entity.getPublishedAt(),
                        entity.getLastError(),
                        entity.getPayloadJson(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
