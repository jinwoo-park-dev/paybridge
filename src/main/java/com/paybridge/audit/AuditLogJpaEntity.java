package com.paybridge.audit;

import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.persistence.AbstractJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_resource", columnList = "resource_type, resource_id"),
                @Index(name = "idx_audit_logs_occurred_at", columnList = "occurred_at")
        }
)
public class AuditLogJpaEntity extends AbstractJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 48)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private AuditOutcome outcome;

    @Column(name = "resource_type", nullable = false, length = 48)
    private String resourceType;

    @Column(name = "resource_id", length = 128)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private PaymentProvider provider;

    @Column(name = "actor_type", nullable = false, length = 32)
    private String actorType;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "detail_json", columnDefinition = "text")
    private String detailJson;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public AuditOutcome getOutcome() { return outcome; }
    public void setOutcome(AuditOutcome outcome) { this.outcome = outcome; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
}
