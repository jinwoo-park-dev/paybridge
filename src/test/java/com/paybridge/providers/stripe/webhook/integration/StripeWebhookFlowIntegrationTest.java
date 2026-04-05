package com.paybridge.providers.stripe.webhook.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.audit.AuditAction;
import com.paybridge.audit.AuditLogJpaRepository;
import com.paybridge.ops.outbox.OutboxEventJpaRepository;
import com.paybridge.ops.outbox.OutboxEventType;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.providers.stripe.StripePaymentConfirmationOutcome;
import com.paybridge.providers.stripe.StripePaymentIntentApplicationService;
import com.paybridge.providers.stripe.webhook.StripeWebhookEventJpaRepository;
import com.paybridge.providers.stripe.webhook.StripeWebhookProcessingStatus;
import com.paybridge.support.test.AbstractPostgresIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StripeWebhookFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_test_paybridge";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StripeWebhookEventJpaRepository stripeWebhookEventJpaRepository;

    @Autowired
    private AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockBean
    private StripePaymentIntentApplicationService stripePaymentIntentApplicationService;

    @DynamicPropertySource
    static void stripeWebhookProperties(DynamicPropertyRegistry registry) {
        registry.add("paybridge.features.stripe-enabled", () -> true);
        registry.add("paybridge.providers.stripe.enabled", () -> true);
        registry.add("paybridge.providers.stripe.webhook-signing-secret", () -> WEBHOOK_SECRET);
    }

    @BeforeEach
    void cleanTables() {
        stripeWebhookEventJpaRepository.deleteAll();
        auditLogJpaRepository.deleteAll();
        outboxEventJpaRepository.deleteAll();
    }

    @Test
    void persistsProcessedWebhookAndSingleOutboxEvent() throws Exception {
        String payload = "{\"id\":\"evt_checkout_123\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_checkout_123\"}}}";
        String signature = validSignature(payload, WEBHOOK_SECRET);

        given(stripePaymentIntentApplicationService.confirmAndRecord("pi_checkout_123"))
                .willReturn(new StripePaymentConfirmationOutcome(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        false,
                        "pi_checkout_123",
                        "succeeded"
                ));

        mockMvc.perform(post("/api/providers/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.eventId").value("evt_checkout_123"))
                .andExpect(jsonPath("$.processingStatus").value("PROCESSED"));

        Optional<com.paybridge.providers.stripe.webhook.StripeWebhookEventJpaEntity> saved =
                stripeWebhookEventJpaRepository.findByProviderAndProviderEventId(PaymentProvider.STRIPE, "evt_checkout_123");
        assertThat(saved).isPresent();
        assertThat(saved.get().getProcessingStatus()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
        assertThat(auditLogJpaRepository.findAll())
                .extracting(log -> log.getAction().name())
                .contains(AuditAction.STRIPE_WEBHOOK_RECEIVED.name(), AuditAction.STRIPE_WEBHOOK_PROCESSED.name());
        assertThat(outboxEventJpaRepository.findAll())
                .extracting(event -> event.getEventType().name())
                .containsExactly(OutboxEventType.STRIPE_WEBHOOK_ACKNOWLEDGED.name());
    }

    @Test
    void marksSecondDeliveryAsDuplicateWithoutAppendingSecondOutboxEvent() throws Exception {
        String payload = "{\"id\":\"evt_duplicate_123\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_duplicate_123\"}}}";
        String signature = validSignature(payload, WEBHOOK_SECRET);

        given(stripePaymentIntentApplicationService.confirmAndRecord("pi_duplicate_123"))
                .willReturn(new StripePaymentConfirmationOutcome(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        false,
                        "pi_duplicate_123",
                        "succeeded"
                ));

        mockMvc.perform(post("/api/providers/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/providers/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.processingStatus").value("DUPLICATE"));

        assertThat(outboxEventJpaRepository.findAll())
                .extracting(event -> event.getEventType().name())
                .containsExactly(OutboxEventType.STRIPE_WEBHOOK_ACKNOWLEDGED.name());
        assertThat(auditLogJpaRepository.findAll())
                .extracting(log -> log.getAction().name())
                .contains(AuditAction.STRIPE_WEBHOOK_DUPLICATE.name());
    }

    private String validSignature(String payload, String secret) {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            return "t=" + timestamp + ",v1=" + java.util.HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
