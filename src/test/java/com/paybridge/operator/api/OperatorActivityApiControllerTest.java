package com.paybridge.operator.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.audit.AuditAction;
import com.paybridge.audit.AuditLogQueryService;
import com.paybridge.audit.AuditLogView;
import com.paybridge.audit.AuditOutcome;
import com.paybridge.ops.outbox.OutboxEventQueryService;
import com.paybridge.ops.outbox.OutboxEventStatus;
import com.paybridge.ops.outbox.OutboxEventType;
import com.paybridge.ops.outbox.OutboxEventView;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.providers.stripe.webhook.StripeWebhookEventView;
import com.paybridge.providers.stripe.webhook.StripeWebhookProcessingStatus;
import com.paybridge.providers.stripe.webhook.StripeWebhookQueryService;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OperatorActivityApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperatorActivityApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogQueryService auditLogQueryService;

    @MockBean
    private OutboxEventQueryService outboxEventQueryService;

    @MockBean
    private StripeWebhookQueryService stripeWebhookQueryService;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void returnsActivityFeeds() throws Exception {
        UUID paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        given(auditLogQueryService.findByPaymentId(paymentId)).willReturn(List.of(
                new AuditLogView(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        AuditAction.PAYMENT_APPROVED,
                        AuditOutcome.SUCCESS,
                        "payment",
                        paymentId.toString(),
                        PaymentProvider.STRIPE,
                        "system",
                        "corr-1",
                        "approved",
                        "{}",
                        OffsetDateTime.of(2026,3,21,12,0,0,0, ZoneOffset.UTC)
                )
        ));
        given(outboxEventQueryService.findByPaymentId(paymentId)).willReturn(List.of(
                new OutboxEventView(
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        "payment",
                        paymentId.toString(),
                        OutboxEventType.PAYMENT_APPROVED,
                        OutboxEventStatus.PENDING,
                        0,
                        OffsetDateTime.of(2026,3,21,12,1,0,0, ZoneOffset.UTC),
                        null,
                        null,
                        "{}",
                        OffsetDateTime.of(2026,3,21,12,1,0,0, ZoneOffset.UTC)
                )
        ));
        given(stripeWebhookQueryService.findRecent()).willReturn(List.of(
                new StripeWebhookEventView(
                        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        PaymentProvider.STRIPE,
                        "evt_123",
                        "payment_intent.succeeded",
                        true,
                        StripeWebhookProcessingStatus.PROCESSED,
                        "corr-2",
                        null,
                        OffsetDateTime.of(2026,3,21,12,2,0,0, ZoneOffset.UTC),
                        OffsetDateTime.of(2026,3,21,12,2,0,0, ZoneOffset.UTC)
                )
        ));

        mockMvc.perform(get("/api/ops/transactions/{paymentId}/audit-logs", paymentId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"PAYMENT_APPROVED\"")));

        mockMvc.perform(get("/api/ops/transactions/{paymentId}/outbox-events", paymentId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"PAYMENT_APPROVED\"")));

        mockMvc.perform(get("/api/ops/stripe-webhooks"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"evt_123\"")));
    }
}
