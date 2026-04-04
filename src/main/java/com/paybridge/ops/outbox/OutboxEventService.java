package com.paybridge.ops.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.payment.domain.Payment;
import com.paybridge.payment.domain.PaymentReversal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OutboxEventService {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventJpaRepository outboxEventJpaRepository, ObjectMapper objectMapper) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.objectMapper = objectMapper;
    }

    public void appendPaymentApproved(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.paymentId());
        payload.put("orderId", payment.orderId());
        payload.put("provider", payment.provider().name());
        payload.put("providerPaymentId", payment.providerPaymentId());
        payload.put("providerTransactionId", payment.providerTransactionId());
        payload.put("amountMinor", payment.amountMinor());
        payload.put("currency", payment.currency());
        payload.put("status", payment.status().name());
        append("payment", payment.paymentId().toString(), OutboxEventType.PAYMENT_APPROVED, payload);
    }

    public void appendFullReversal(Payment payment, PaymentReversal reversal) {
        appendReversal(payment, reversal, OutboxEventType.PAYMENT_FULLY_REVERSED);
    }

    public void appendPartialReversal(Payment payment, PaymentReversal reversal) {
        appendReversal(payment, reversal, OutboxEventType.PAYMENT_PARTIALLY_REVERSED);
    }

    public void appendStripeWebhookAcknowledged(String eventId, String eventType, String processingStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("providerEventId", eventId);
        payload.put("eventType", eventType);
        payload.put("processingStatus", processingStatus);
        append("stripe_webhook", eventId, OutboxEventType.STRIPE_WEBHOOK_ACKNOWLEDGED, payload);
    }

    private void appendReversal(Payment payment, PaymentReversal reversal, OutboxEventType eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.paymentId());
        payload.put("reversalId", reversal.reversalId());
        payload.put("provider", payment.provider().name());
        payload.put("reversalType", reversal.reversalType().name());
        payload.put("amountMinor", reversal.amountMinor());
        payload.put("remainingAmountMinor", reversal.remainingAmountMinor());
        payload.put("providerReversalId", reversal.providerReversalId());
        payload.put("paymentStatus", payment.status().name());
        append("payment", payment.paymentId().toString(), eventType, payload);
    }

    private void append(String aggregateType, String aggregateId, OutboxEventType eventType, Map<String, Object> payload) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType(aggregateType);
        entity.setAggregateId(aggregateId);
        entity.setEventType(eventType);
        entity.setPayloadJson(writeJson(payload));
        entity.setStatus(OutboxEventStatus.PENDING);
        entity.setRetryCount(0);
        entity.setAvailableAt(OffsetDateTime.now(ZoneOffset.UTC));
        outboxEventJpaRepository.save(entity);
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
