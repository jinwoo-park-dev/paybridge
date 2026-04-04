package com.paybridge.payment.persistence;

import com.paybridge.payment.domain.Payment;
import com.paybridge.payment.domain.PaymentReversal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceMapper {

    public Payment toDomain(PaymentJpaEntity entity) {
        return Payment.rehydrate(
                entity.getId(),
                entity.getOrderId(),
                entity.getProvider(),
                entity.getProviderPaymentId(),
                entity.getProviderTransactionId(),
                entity.getAmountMinor(),
                entity.getReversibleAmountMinor(),
                entity.isPartialReversalSupported(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getApprovedAt() == null ? null : entity.getApprovedAt().toInstant(),
                entity.getCreatedAt().toInstant(),
                entity.getUpdatedAt().toInstant()
        );
    }

    public PaymentJpaEntity toNewEntity(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        apply(entity, payment);
        return entity;
    }

    public void apply(PaymentJpaEntity entity, Payment payment) {
        entity.setId(payment.paymentId());
        entity.setOrderId(payment.orderId());
        entity.setProvider(payment.provider());
        entity.setProviderPaymentId(payment.providerPaymentId());
        entity.setProviderTransactionId(payment.providerTransactionId());
        entity.setAmountMinor(payment.amountMinor());
        entity.setReversibleAmountMinor(payment.reversibleAmountMinor());
        entity.setPartialReversalSupported(payment.partialReversalSupported());
        entity.setCurrency(payment.currency());
        entity.setStatus(payment.status());
        entity.setApprovedAt(toOffsetDateTime(payment.approvedAt()));
        entity.setCreatedAt(toOffsetDateTime(payment.createdAt()));
        entity.setUpdatedAt(toOffsetDateTime(payment.updatedAt()));
    }

    public PaymentReversalJpaEntity toNewEntity(PaymentReversal reversal) {
        PaymentReversalJpaEntity entity = new PaymentReversalJpaEntity();
        entity.setId(reversal.reversalId());
        entity.setPaymentId(reversal.paymentId());
        entity.setReversalType(reversal.reversalType());
        entity.setStatus(reversal.reversalStatus());
        entity.setAmountMinor(reversal.amountMinor());
        entity.setRemainingAmountMinor(reversal.remainingAmountMinor());
        entity.setReason(reversal.reason());
        entity.setProviderReversalId(reversal.providerReversalId());
        entity.setProcessedAt(toOffsetDateTime(reversal.processedAt()));
        entity.setCreatedAt(toOffsetDateTime(reversal.processedAt()));
        entity.setUpdatedAt(toOffsetDateTime(reversal.processedAt()));
        return entity;
    }

    public PaymentReversal toDomain(PaymentReversalJpaEntity entity) {
        return new PaymentReversal(
                entity.getId(),
                entity.getPaymentId(),
                entity.getReversalType(),
                entity.getStatus(),
                entity.getAmountMinor(),
                entity.getRemainingAmountMinor(),
                entity.getReason(),
                entity.getProviderReversalId(),
                entity.getProcessedAt().toInstant()
        );
    }

    private OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
