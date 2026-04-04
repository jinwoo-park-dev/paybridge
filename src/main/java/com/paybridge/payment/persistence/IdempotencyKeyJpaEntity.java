package com.paybridge.payment.persistence;

import com.paybridge.payment.domain.IdempotencyOperation;
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
        name = "idempotency_keys",
        indexes = {
                @Index(name = "idx_idempotency_operation_key", columnList = "operation, idempotency_key", unique = true),
                @Index(name = "idx_idempotency_payment_id", columnList = "result_payment_id")
        }
)
public class IdempotencyKeyJpaEntity extends AbstractJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 32)
    private IdempotencyOperation operation;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private IdempotencyRecordStatus status;

    @Column(name = "locked_until", nullable = false)
    private OffsetDateTime lockedUntil;

    @Column(name = "result_payment_id")
    private UUID resultPaymentId;

    public IdempotencyKeyJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public IdempotencyOperation getOperation() {
        return operation;
    }

    public void setOperation(IdempotencyOperation operation) {
        this.operation = operation;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public IdempotencyRecordStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyRecordStatus status) {
        this.status = status;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(OffsetDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public UUID getResultPaymentId() {
        return resultPaymentId;
    }

    public void setResultPaymentId(UUID resultPaymentId) {
        this.resultPaymentId = resultPaymentId;
    }
}
