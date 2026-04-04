package com.paybridge.ops.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findTop20ByStatusOrderByAvailableAtAsc(OutboxEventStatus status);

    List<OutboxEventJpaEntity> findTop50ByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(String aggregateType, String aggregateId);
}
