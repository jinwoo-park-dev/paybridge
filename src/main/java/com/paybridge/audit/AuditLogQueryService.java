package com.paybridge.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final AuditLogJpaRepository auditLogJpaRepository;

    public AuditLogQueryService(AuditLogJpaRepository auditLogJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
    }

    public List<AuditLogView> findByPaymentId(UUID paymentId) {
        return auditLogJpaRepository.findTop50ByResourceTypeAndResourceIdOrderByOccurredAtDesc("payment", paymentId.toString())
                .stream()
                .map(entity -> new AuditLogView(
                        entity.getId(),
                        entity.getAction(),
                        entity.getOutcome(),
                        entity.getResourceType(),
                        entity.getResourceId(),
                        entity.getProvider(),
                        entity.getActorType(),
                        entity.getCorrelationId(),
                        entity.getMessage(),
                        entity.getDetailJson(),
                        entity.getOccurredAt()
                ))
                .toList();
    }
}
