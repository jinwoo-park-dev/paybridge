package com.paybridge.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.paybridge.audit.AuditLogService;
import com.paybridge.ops.outbox.OutboxEventService;
import com.paybridge.payment.domain.Payment;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.ReversalType;
import com.paybridge.payment.persistence.PaymentJpaEntity;
import com.paybridge.payment.persistence.PaymentJpaRepository;
import com.paybridge.payment.persistence.PaymentPersistenceMapper;
import com.paybridge.payment.persistence.PaymentReversalJpaEntity;
import com.paybridge.payment.persistence.PaymentReversalJpaRepository;
import com.paybridge.support.metrics.PaymentMetricsRecorder;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentCommandApplicationServiceTest {

    @Test
    void appendsAuditAndOutboxOnApprovalAndPartialReversal() {
        PaymentJpaRepository paymentRepository = org.mockito.Mockito.mock(PaymentJpaRepository.class);
        PaymentReversalJpaRepository reversalRepository = org.mockito.Mockito.mock(PaymentReversalJpaRepository.class);
        PaymentPersistenceMapper mapper = new PaymentPersistenceMapper();
        AuditLogService auditLogService = org.mockito.Mockito.mock(AuditLogService.class);
        OutboxEventService outboxEventService = org.mockito.Mockito.mock(OutboxEventService.class);
        PaymentMetricsRecorder metricsRecorder = org.mockito.Mockito.mock(PaymentMetricsRecorder.class);

        PaymentCommandApplicationService service = new PaymentCommandApplicationService(
                paymentRepository,
                reversalRepository,
                mapper,
                auditLogService,
                outboxEventService,
                metricsRecorder
        );

        given(paymentRepository.saveAndFlush(any(PaymentJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(paymentRepository.save(any(PaymentJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(reversalRepository.saveAndFlush(any(PaymentReversalJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(reversalRepository.findByPaymentIdAndProviderReversalId(any(UUID.class), any(String.class))).willReturn(Optional.empty());

        UUID paymentId = service.recordApprovedPayment(new CreateApprovedPaymentCommand(
                "ORDER-1001",
                PaymentProvider.STRIPE,
                "pi_123",
                "ch_123",
                1000L,
                true,
                "USD",
                Instant.parse("2026-03-20T00:00:00Z")
        ));

        ArgumentCaptor<Payment> approvedCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(outboxEventService).appendPaymentApproved(approvedCaptor.capture());
        assertThat(approvedCaptor.getValue().orderId()).isEqualTo("ORDER-1001");
        verify(metricsRecorder).recordPaymentApproved(PaymentProvider.STRIPE);

        PaymentJpaEntity existing = mapper.toNewEntity(Payment.approved(
                paymentId,
                "ORDER-1001",
                PaymentProvider.STRIPE,
                "pi_123",
                "ch_123",
                1000L,
                true,
                "USD",
                Instant.parse("2026-03-20T00:00:00Z")
        ));
        given(paymentRepository.findByIdForUpdate(paymentId)).willReturn(Optional.of(existing));

        UUID reversalId = service.recordPartialReversal(new RegisterPartialReversalCommand(
                paymentId,
                300L,
                "customer changed order",
                "re_123",
                Instant.parse("2026-03-20T00:10:00Z")
        ));

        assertThat(reversalId).isNotNull();
        verify(outboxEventService).appendPartialReversal(any(Payment.class), any(com.paybridge.payment.domain.PaymentReversal.class));
        verify(metricsRecorder).recordReversal(PaymentProvider.STRIPE, ReversalType.PARTIAL.name());
        verify(auditLogService, org.mockito.Mockito.atLeastOnce()).success(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void reusesExistingPaymentWhenProviderIdentifiersAlreadyExist() {
        PaymentJpaRepository paymentRepository = org.mockito.Mockito.mock(PaymentJpaRepository.class);
        PaymentReversalJpaRepository reversalRepository = org.mockito.Mockito.mock(PaymentReversalJpaRepository.class);
        PaymentPersistenceMapper mapper = new PaymentPersistenceMapper();
        AuditLogService auditLogService = org.mockito.Mockito.mock(AuditLogService.class);
        OutboxEventService outboxEventService = org.mockito.Mockito.mock(OutboxEventService.class);
        PaymentMetricsRecorder metricsRecorder = org.mockito.Mockito.mock(PaymentMetricsRecorder.class);

        PaymentCommandApplicationService service = new PaymentCommandApplicationService(
                paymentRepository,
                reversalRepository,
                mapper,
                auditLogService,
                outboxEventService,
                metricsRecorder
        );

        UUID existingPaymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PaymentJpaEntity existing = mapper.toNewEntity(Payment.approved(
                existingPaymentId,
                "ORDER-1001",
                PaymentProvider.STRIPE,
                "pi_existing",
                "ch_existing",
                1000L,
                true,
                "USD",
                Instant.parse("2026-03-20T00:00:00Z")
        ));

        given(paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.STRIPE, "pi_existing")).willReturn(Optional.of(existing));

        UUID paymentId = service.recordApprovedPayment(new CreateApprovedPaymentCommand(
                "ORDER-1001",
                PaymentProvider.STRIPE,
                "pi_existing",
                "ch_existing",
                1000L,
                true,
                "USD",
                Instant.parse("2026-03-20T00:00:00Z")
        ));

        assertThat(paymentId).isEqualTo(existingPaymentId);
        verify(outboxEventService, never()).appendPaymentApproved(any());
        verify(metricsRecorder, never()).recordPaymentApproved(any(PaymentProvider.class));
        verify(auditLogService, never()).success(any(), any(), any(), any(), any(), any(), any());
    }
}
