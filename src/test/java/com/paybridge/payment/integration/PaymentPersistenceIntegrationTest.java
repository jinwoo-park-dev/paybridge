package com.paybridge.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.paybridge.audit.AuditAction;
import com.paybridge.audit.AuditLogJpaRepository;
import com.paybridge.ops.outbox.OutboxEventJpaRepository;
import com.paybridge.ops.outbox.OutboxEventType;
import com.paybridge.payment.application.CreateApprovedPaymentCommand;
import com.paybridge.payment.application.PaymentCommandApplicationService;
import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.TransactionExportCriteria;
import com.paybridge.payment.application.TransactionExportPageView;
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
    void exportsMachineReadableRowsWithApprovedWindowPagingAndFilters() {
        paymentCommandApplicationService.recordApprovedPayment(
                new CreateApprovedPaymentCommand(
                        "ORD-EXP-1001",
                        PaymentProvider.STRIPE,
                        "pi_exp_1001",
                        "ch_exp_1001",
                        1_999L,
                        true,
                        "USD",
                        Instant.parse("2026-03-20T01:00:00Z")
                )
        );

        UUID filteredPaymentId = paymentCommandApplicationService.recordApprovedPayment(
                new CreateApprovedPaymentCommand(
                        "ORD-EXP-1002",
                        PaymentProvider.NICEPAY,
                        "tid_exp_1002",
                        "auth_exp_1002",
                        12_000L,
                        true,
                        "KRW",
                        Instant.parse("2026-03-20T02:00:00Z")
                )
        );

        paymentCommandApplicationService.recordPartialReversal(
                new RegisterPartialReversalCommand(
                        filteredPaymentId,
                        2_000L,
                        "settlement mismatch investigation",
                        "cancel-exp-1002",
                        Instant.parse("2026-03-20T02:10:00Z")
                )
        );

        paymentCommandApplicationService.recordApprovedPayment(
                new CreateApprovedPaymentCommand(
                        "ORD-EXP-1003",
                        PaymentProvider.STRIPE,
                        "pi_exp_1003",
                        "ch_exp_1003",
                        2_999L,
                        true,
                        "USD",
                        Instant.parse("2026-03-20T03:00:00Z")
                )
        );

        TransactionExportPageView firstPage = paymentQueryApplicationService.export(
                new TransactionExportCriteria(
                        Instant.parse("2026-03-20T00:30:00Z"),
                        Instant.parse("2026-03-20T03:30:00Z"),
                        null,
                        null
                ),
                0,
                2
        );

        assertThat(firstPage.page()).isEqualTo(0);
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.content())
                .extracting(row -> row.orderId())
                .containsExactly("ORD-EXP-1003", "ORD-EXP-1002");
        assertThat(firstPage.content().getFirst().amountMinor()).isEqualTo(2_999L);
        assertThat(firstPage.content().getFirst().approvedAt()).isEqualTo(Instant.parse("2026-03-20T03:00:00Z"));

        TransactionExportPageView filteredPage = paymentQueryApplicationService.export(
                new TransactionExportCriteria(
                        Instant.parse("2026-03-20T00:30:00Z"),
                        Instant.parse("2026-03-20T03:30:00Z"),
                        PaymentProvider.NICEPAY,
                        PaymentStatus.PARTIALLY_REVERSED
                ),
                0,
                10
        );

        assertThat(filteredPage.hasNext()).isFalse();
        assertThat(filteredPage.content()).singleElement().satisfies(row -> {
            assertThat(row.paymentId()).isEqualTo(filteredPaymentId);
            assertThat(row.orderId()).isEqualTo("ORD-EXP-1002");
            assertThat(row.provider()).isEqualTo(PaymentProvider.NICEPAY);
            assertThat(row.status()).isEqualTo(PaymentStatus.PARTIALLY_REVERSED);
            assertThat(row.amountMinor()).isEqualTo(12_000L);
            assertThat(row.reversibleAmountMinor()).isEqualTo(10_000L);
            assertThat(row.currency()).isEqualTo("KRW");
            assertThat(row.providerPaymentId()).isEqualTo("tid_exp_1002");
            assertThat(row.providerTransactionId()).isEqualTo("auth_exp_1002");
            assertThat(row.approvedAt()).isEqualTo(Instant.parse("2026-03-20T02:00:00Z"));
            assertThat(row.updatedAt()).isEqualTo(Instant.parse("2026-03-20T02:10:00Z"));
        });
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
