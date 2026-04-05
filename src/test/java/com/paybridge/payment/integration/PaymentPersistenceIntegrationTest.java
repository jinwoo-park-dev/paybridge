package com.paybridge.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.paybridge.audit.AuditAction;
import com.paybridge.audit.AuditLogJpaRepository;
import com.paybridge.ops.outbox.OutboxEventJpaRepository;
import com.paybridge.ops.outbox.OutboxEventType;
import com.paybridge.payment.application.CreateApprovedPaymentCommand;
import com.paybridge.payment.application.PaymentCommandApplicationService;
import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.application.RegisterPartialReversalCommand;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import com.paybridge.payment.persistence.IdempotencyKeyJpaRepository;
import com.paybridge.payment.persistence.PaymentJpaRepository;
import com.paybridge.payment.persistence.PaymentReversalJpaRepository;
import com.paybridge.support.test.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PaymentCommandApplicationService paymentCommandApplicationService;

    @Autowired
    private PaymentQueryApplicationService paymentQueryApplicationService;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentReversalJpaRepository paymentReversalJpaRepository;

    @Autowired
    private IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @Autowired
    private AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentReversalJpaRepository.deleteAll();
        paymentJpaRepository.deleteAll();
        idempotencyKeyJpaRepository.deleteAll();
        auditLogJpaRepository.deleteAll();
        outboxEventJpaRepository.deleteAll();
    }

    @Test
    void persistsApprovedPaymentAndPartialReversalWithAuditAndOutbox() {
        UUID paymentId = paymentCommandApplicationService.recordApprovedPayment(
                new CreateApprovedPaymentCommand(
                        "ORD-INT-1001",
                        PaymentProvider.NICEPAY,
                        "tid-int-1001",
                        "auth-int-1001",
                        12_000L,
                        true,
                        "KRW",
                        Instant.parse("2026-03-20T01:00:00Z")
                )
        );

        paymentCommandApplicationService.recordPartialReversal(
                new RegisterPartialReversalCommand(
                        paymentId,
                        2_000L,
                        "customer requested partial cancellation",
                        "cancel-int-1001",
                        Instant.parse("2026-03-20T01:05:00Z")
                )
        );

        PaymentDetailView detail = paymentQueryApplicationService.getDetail(paymentId);

        assertThat(detail.status()).isEqualTo(PaymentStatus.PARTIALLY_REVERSED);
        assertThat(detail.reversals()).hasSize(1);
        assertThat(detail.reversibleAmountDisplay()).contains("10,000");
        assertThat(paymentJpaRepository.findAll()).hasSize(1);
        assertThat(paymentReversalJpaRepository.findAll()).hasSize(1);
        assertThat(auditLogJpaRepository.findAll())
                .extracting(log -> log.getAction().name())
                .containsExactlyInAnyOrder(
                        AuditAction.PAYMENT_APPROVED.name(),
                        AuditAction.PAYMENT_PARTIAL_REVERSAL_RECORDED.name()
                );
        assertThat(outboxEventJpaRepository.findAll())
                .extracting(event -> event.getEventType().name())
                .containsExactlyInAnyOrder(
                        OutboxEventType.PAYMENT_APPROVED.name(),
                        OutboxEventType.PAYMENT_PARTIALLY_REVERSED.name()
                );
    }
}
