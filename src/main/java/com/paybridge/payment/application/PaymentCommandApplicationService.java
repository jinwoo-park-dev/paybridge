package com.paybridge.payment.application;

import com.paybridge.audit.AuditAction;
import com.paybridge.audit.AuditLogService;
import com.paybridge.ops.outbox.OutboxEventService;
import com.paybridge.payment.domain.Payment;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.RegisteredReversal;
import com.paybridge.payment.persistence.PaymentJpaEntity;
import com.paybridge.payment.persistence.PaymentJpaRepository;
import com.paybridge.payment.persistence.PaymentPersistenceMapper;
import com.paybridge.payment.persistence.PaymentReversalJpaEntity;
import com.paybridge.payment.persistence.PaymentReversalJpaRepository;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import com.paybridge.support.metrics.PaymentMetricsRecorder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentCommandApplicationService {

    private final PaymentJpaRepository paymentRepository;
    private final PaymentReversalJpaRepository paymentReversalRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;
    private final AuditLogService auditLogService;
    private final OutboxEventService outboxEventService;
    private final PaymentMetricsRecorder paymentMetricsRecorder;

    public PaymentCommandApplicationService(
            PaymentJpaRepository paymentRepository,
            PaymentReversalJpaRepository paymentReversalRepository,
            PaymentPersistenceMapper paymentPersistenceMapper,
            AuditLogService auditLogService,
            OutboxEventService outboxEventService,
            PaymentMetricsRecorder paymentMetricsRecorder
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentReversalRepository = paymentReversalRepository;
        this.paymentPersistenceMapper = paymentPersistenceMapper;
        this.auditLogService = auditLogService;
        this.outboxEventService = outboxEventService;
        this.paymentMetricsRecorder = paymentMetricsRecorder;
    }

    public UUID recordApprovedPayment(CreateApprovedPaymentCommand command) {
        Payment payment = Payment.approved(
                UUID.randomUUID(),
                command.orderId(),
                command.provider(),
                command.providerPaymentId(),
                command.providerTransactionId(),
                command.amountMinor(),
                command.partialReversalSupported(),
                command.currency(),
                command.approvedAt() == null ? Instant.now() : command.approvedAt()
        );

        Optional<UUID> existingPaymentId = findExistingApprovedPaymentId(payment);
        if (existingPaymentId.isPresent()) {
            return existingPaymentId.get();
        }

        paymentRepository.saveAndFlush(paymentPersistenceMapper.toNewEntity(payment));

        outboxEventService.appendPaymentApproved(payment);
        paymentMetricsRecorder.recordPaymentApproved(payment.provider());
        auditLogService.success(
                AuditAction.PAYMENT_APPROVED,
                "payment",
                payment.paymentId().toString(),
                payment.provider(),
                actorTypeFor(payment.provider()),
                "Payment approval recorded.",
                Map.of(
                        "orderId", payment.orderId(),
                        "amountMinor", payment.amountMinor(),
                        "currency", payment.currency(),
                        "providerPaymentId", nullSafe(payment.providerPaymentId()),
                        "providerTransactionId", nullSafe(payment.providerTransactionId())
                )
        );
        return payment.paymentId();
    }

    public UUID recordFullReversal(RegisterFullReversalCommand command) {
        Optional<UUID> existingReversalId = findExistingReversalId(command.paymentId(), command.providerReversalId());
        if (existingReversalId.isPresent()) {
            return existingReversalId.get();
        }

        PaymentJpaEntity entity = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> notFound(command.paymentId()));
        Payment payment = paymentPersistenceMapper.toDomain(entity);
        RegisteredReversal registeredReversal = payment.registerFullReversal(
                UUID.randomUUID(),
                command.reason(),
                command.providerReversalId(),
                command.processedAt() == null ? Instant.now() : command.processedAt()
        );

        paymentPersistenceMapper.apply(entity, registeredReversal.updatedPayment());
        paymentRepository.save(entity);
        paymentReversalRepository.saveAndFlush(paymentPersistenceMapper.toNewEntity(registeredReversal.reversal()));
        outboxEventService.appendFullReversal(registeredReversal.updatedPayment(), registeredReversal.reversal());
        paymentMetricsRecorder.recordReversal(payment.provider(), registeredReversal.reversal().reversalType().name());
        auditLogService.success(
                AuditAction.PAYMENT_FULL_REVERSAL_RECORDED,
                "payment",
                payment.paymentId().toString(),
                payment.provider(),
                actorTypeFor(payment.provider()),
                "Full reversal recorded.",
                reversalDetail(registeredReversal)
        );
        return registeredReversal.reversal().reversalId();
    }

    public UUID recordPartialReversal(RegisterPartialReversalCommand command) {
        Optional<UUID> existingReversalId = findExistingReversalId(command.paymentId(), command.providerReversalId());
        if (existingReversalId.isPresent()) {
            return existingReversalId.get();
        }

        PaymentJpaEntity entity = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> notFound(command.paymentId()));
        Payment payment = paymentPersistenceMapper.toDomain(entity);
        RegisteredReversal registeredReversal = payment.registerPartialReversal(
                UUID.randomUUID(),
                command.amountMinor(),
                command.reason(),
                command.providerReversalId(),
                command.processedAt() == null ? Instant.now() : command.processedAt()
        );

        paymentPersistenceMapper.apply(entity, registeredReversal.updatedPayment());
        paymentRepository.save(entity);
        paymentReversalRepository.saveAndFlush(paymentPersistenceMapper.toNewEntity(registeredReversal.reversal()));
        outboxEventService.appendPartialReversal(registeredReversal.updatedPayment(), registeredReversal.reversal());
        paymentMetricsRecorder.recordReversal(payment.provider(), registeredReversal.reversal().reversalType().name());
        auditLogService.success(
                AuditAction.PAYMENT_PARTIAL_REVERSAL_RECORDED,
                "payment",
                payment.paymentId().toString(),
                payment.provider(),
                actorTypeFor(payment.provider()),
                "Partial reversal recorded.",
                reversalDetail(registeredReversal)
        );
        return registeredReversal.reversal().reversalId();
    }

    private Map<String, Object> reversalDetail(RegisteredReversal registeredReversal) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("reversalId", registeredReversal.reversal().reversalId());
        detail.put("reversalType", registeredReversal.reversal().reversalType().name());
        detail.put("amountMinor", registeredReversal.reversal().amountMinor());
        detail.put("remainingAmountMinor", registeredReversal.reversal().remainingAmountMinor());
        detail.put("providerReversalId", nullSafe(registeredReversal.reversal().providerReversalId()));
        return detail;
    }

    private Optional<UUID> findExistingApprovedPaymentId(Payment payment) {
        Optional<PaymentJpaEntity> byProviderPaymentId = optionalText(payment.providerPaymentId())
                .flatMap(providerPaymentId -> paymentRepository.findByProviderAndProviderPaymentId(payment.provider(), providerPaymentId));
        if (byProviderPaymentId.isPresent()) {
            return byProviderPaymentId.map(PaymentJpaEntity::getId);
        }

        return optionalText(payment.providerTransactionId())
                .flatMap(providerTransactionId -> paymentRepository.findByProviderAndProviderTransactionId(payment.provider(), providerTransactionId))
                .map(PaymentJpaEntity::getId);
    }

    private Optional<UUID> findExistingReversalId(UUID paymentId, String providerReversalId) {
        return optionalText(providerReversalId)
                .flatMap(normalizedProviderReversalId -> paymentReversalRepository.findByPaymentIdAndProviderReversalId(paymentId, normalizedProviderReversalId))
                .map(PaymentReversalJpaEntity::getId);
    }

    private Optional<String> optionalText(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private String actorTypeFor(PaymentProvider provider) {
        return provider == PaymentProvider.STRIPE ? "stripe" : provider == PaymentProvider.NICEPAY ? "nicepay" : "system";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private PayBridgeException notFound(UUID paymentId) {
        return new PayBridgeException(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                "Payment not found: " + paymentId
        );
    }
}
