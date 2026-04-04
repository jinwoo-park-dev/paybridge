package com.paybridge.payment.persistence;

import com.paybridge.payment.domain.ReversalStatus;
import com.paybridge.payment.domain.ReversalType;
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
        name = "payment_reversals",
        indexes = {
                @Index(name = "idx_payment_reversals_payment_id", columnList = "payment_id"),
                @Index(name = "idx_payment_reversals_type_created_at", columnList = "reversal_type, created_at")
        }
)
public class PaymentReversalJpaEntity extends AbstractJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reversal_type", nullable = false, length = 16)
    private ReversalType reversalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReversalStatus status;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "remaining_amount_minor", nullable = false)
    private long remainingAmountMinor;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "provider_reversal_id", length = 128)
    private String providerReversalId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    protected PaymentReversalJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public ReversalType getReversalType() {
        return reversalType;
    }

    public void setReversalType(ReversalType reversalType) {
        this.reversalType = reversalType;
    }

    public ReversalStatus getStatus() {
        return status;
    }

    public void setStatus(ReversalStatus status) {
        this.status = status;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public long getRemainingAmountMinor() {
        return remainingAmountMinor;
    }

    public void setRemainingAmountMinor(long remainingAmountMinor) {
        this.remainingAmountMinor = remainingAmountMinor;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getProviderReversalId() {
        return providerReversalId;
    }

    public void setProviderReversalId(String providerReversalId) {
        this.providerReversalId = providerReversalId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
