package com.paybridge.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.support.web.RequestCorrelationFilter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogJpaRepository auditLogJpaRepository, ObjectMapper objectMapper) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.objectMapper = objectMapper;
    }

    public void info(
            AuditAction action,
            String resourceType,
            String resourceId,
            PaymentProvider provider,
            String actorType,
            String message,
            Map<String, ?> detail
    ) {
        save(action, AuditOutcome.INFO, resourceType, resourceId, provider, actorType, message, detail);
    }

    public void success(
            AuditAction action,
            String resourceType,
            String resourceId,
            PaymentProvider provider,
            String actorType,
            String message,
            Map<String, ?> detail
    ) {
        save(action, AuditOutcome.SUCCESS, resourceType, resourceId, provider, actorType, message, detail);
    }

    public void failure(
            AuditAction action,
            String resourceType,
            String resourceId,
            PaymentProvider provider,
            String actorType,
            String message,
            Map<String, ?> detail
    ) {
        save(action, AuditOutcome.FAILURE, resourceType, resourceId, provider, actorType, message, detail);
    }

    private void save(
            AuditAction action,
            AuditOutcome outcome,
            String resourceType,
            String resourceId,
            PaymentProvider provider,
            String actorType,
            String message,
            Map<String, ?> detail
    ) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setAction(action);
        entity.setOutcome(outcome);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setProvider(provider);
        entity.setActorType(actorType == null || actorType.isBlank() ? "system" : actorType.trim());
        entity.setCorrelationId(MDC.get(RequestCorrelationFilter.CORRELATION_ID_MDC_KEY));
        entity.setMessage(message);
        entity.setDetailJson(writeJson(detail));
        entity.setOccurredAt(OffsetDateTime.now(ZoneOffset.UTC));
        auditLogJpaRepository.save(entity);
    }

    private String writeJson(Map<String, ?> detail) {
        if (detail == null || detail.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
