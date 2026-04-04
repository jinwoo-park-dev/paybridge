package com.paybridge.payment.persistence;

import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
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
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order_id", columnList = "order_id"),
                @Index(name = "idx_payments_provider_payment_id", columnList = "provider_payment_id"),
                @Index(name = "idx_payments_provider_transaction_id", columnList = "provider_transaction_id"),
                @Index(name = "idx_payments_status_created_at", columnList = "status, created_at")
        }
)
public class PaymentJpaEntity extends AbstractJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "reversible_amount_minor", nullable = false)
    private long reversibleAmountMinor;

    @Column(name = "partial_reversal_supported", nullable = false)
    private boolean partialReversalSupported = true;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "provider_payment_id", length = 128)
    private String providerPaymentId;

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    protected PaymentJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public long getReversibleAmountMinor() {
        return reversibleAmountMinor;
    }

    public void setReversibleAmountMinor(long reversibleAmountMinor) {
        this.reversibleAmountMinor = reversibleAmountMinor;
    }

    public boolean isPartialReversalSupported() {
        return partialReversalSupported;
    }

    public void setPartialReversalSupported(boolean partialReversalSupported) {
        this.partialReversalSupported = partialReversalSupported;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
