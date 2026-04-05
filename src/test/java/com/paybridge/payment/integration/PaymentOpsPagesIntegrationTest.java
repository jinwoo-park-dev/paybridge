package com.paybridge.payment.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.payment.application.CreateApprovedPaymentCommand;
import com.paybridge.payment.application.PaymentCommandApplicationService;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.persistence.IdempotencyKeyJpaRepository;
import com.paybridge.payment.persistence.PaymentJpaRepository;
import com.paybridge.payment.persistence.PaymentReversalJpaRepository;
import com.paybridge.support.test.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentOpsPagesIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentCommandApplicationService paymentCommandApplicationService;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentReversalJpaRepository paymentReversalJpaRepository;

    @Autowired
    private IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentReversalJpaRepository.deleteAll();
        paymentJpaRepository.deleteAll();
        idempotencyKeyJpaRepository.deleteAll();
    }

    @Test
    void rendersSearchAndDetailPagesAgainstPostgresContainer() throws Exception {
        UUID paymentId = paymentCommandApplicationService.recordApprovedPayment(
                new CreateApprovedPaymentCommand(
                        "ORD-OPS-1001",
                        PaymentProvider.STRIPE,
                        "pi_ops_1001",
                        "ch_ops_1001",
                        1_999L,
                        true,
                        "USD",
                        Instant.parse("2026-03-20T02:00:00Z")
                )
        );

        mockMvc.perform(get("/ops/transactions/search").with(user("operator").roles("OPERATOR")).param("orderId", "ORD-OPS-1001"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Transaction Search")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ORD-OPS-1001")));

        mockMvc.perform(get("/payments/{paymentId}", paymentId).with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment Detail")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stripe refund actions")));
    }
}
