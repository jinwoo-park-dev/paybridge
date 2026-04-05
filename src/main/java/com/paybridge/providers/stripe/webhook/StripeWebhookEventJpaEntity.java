package com.paybridge.providers.stripe.webhook;

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
        name = "stripe_webhook_events",
        indexes = {
                @Index(name = "idx_stripe_webhook_status_created_at", columnList = "processing_status, created_at")
        }
)
public class StripeWebhookEventJpaEntity extends AbstractJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "provider_event_id", nullable = false, length = 128)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Column(name = "signature_verified", nullable = false)
    private boolean signatureVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private StripeWebhookProcessingStatus processingStatus;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }
    public String getProviderEventId() { return providerEventId; }
    public void setProviderEventId(String providerEventId) { this.providerEventId = providerEventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayloadSha256() { return payloadSha256; }
    public void setPayloadSha256(String payloadSha256) { this.payloadSha256 = payloadSha256; }
    public boolean isSignatureVerified() { return signatureVerified; }
    public void setSignatureVerified(boolean signatureVerified) { this.signatureVerified = signatureVerified; }
    public StripeWebhookProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(StripeWebhookProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
