package com.paybridge.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    List<AuditLogJpaEntity> findTop50ByResourceTypeAndResourceIdOrderByOccurredAtDesc(String resourceType, String resourceId);
}
