package com.paybridge.providers.stripe.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.audit.AuditLogService;
import com.paybridge.ops.outbox.OutboxEventService;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.providers.stripe.StripePaymentConfirmationOutcome;
import com.paybridge.providers.stripe.StripePaymentIntentApplicationService;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.PayBridgeException;
import com.paybridge.support.metrics.PaymentMetricsRecorder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class StripeWebhookApplicationServiceTest {

    private final PayBridgeProperties properties = configuredProperties();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StripeWebhookEventJpaRepository webhookEventRepository = org.mockito.Mockito.mock(StripeWebhookEventJpaRepository.class);
    private final StripePaymentIntentApplicationService confirmationService = org.mockito.Mockito.mock(StripePaymentIntentApplicationService.class);
    private final AuditLogService auditLogService = org.mockito.Mockito.mock(AuditLogService.class);
    private final OutboxEventService outboxEventService = org.mockito.Mockito.mock(OutboxEventService.class);
    private final PaymentMetricsRecorder metricsRecorder = org.mockito.Mockito.mock(PaymentMetricsRecorder.class);

    @Test
    void processesSucceededPaymentIntentWebhook() {
        StripeWebhookApplicationService service = new StripeWebhookApplicationService(
                properties,
                objectMapper,
                webhookEventRepository,
                confirmationService,
                auditLogService,
                outboxEventService,
                metricsRecorder
        );
        String payload = "{\"id\":\"evt_123\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_123\"}}}";
        String signature = validSignature(payload, properties.getProviders().getStripe().getWebhookSigningSecret());
        given(webhookEventRepository.saveAndFlush(any(StripeWebhookEventJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(confirmationService.confirmAndRecord("pi_123"))
                .willReturn(new StripePaymentConfirmationOutcome(UUID.fromString("11111111-1111-1111-1111-111111111111"), false, "pi_123", "succeeded", "ORD-STR-2026-1001", "USD 19.99", "USD", "ch_test_123"));

        StripeWebhookOutcome outcome = service.handle(payload, signature);

        assertThat(outcome.duplicate()).isFalse();
        assertThat(outcome.processingStatus()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
        verify(confirmationService).confirmAndRecord("pi_123");
        verify(outboxEventService).appendStripeWebhookAcknowledged("evt_123", "payment_intent.succeeded", "PROCESSED");
    }

    @Test
    void returnsDuplicateWhenEventAlreadyExists() {
        StripeWebhookApplicationService service = new StripeWebhookApplicationService(
                properties,
                objectMapper,
                webhookEventRepository,
                confirmationService,
                auditLogService,
                outboxEventService,
                metricsRecorder
        );
        String payload = "{\"id\":\"evt_dup\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_123\"}}}";
        String signature = validSignature(payload, properties.getProviders().getStripe().getWebhookSigningSecret());

        StripeWebhookEventJpaEntity existing = new StripeWebhookEventJpaEntity();
        existing.setId(UUID.randomUUID());
        existing.setProvider(PaymentProvider.STRIPE);
        existing.setProviderEventId("evt_dup");
        existing.setEventType("payment_intent.succeeded");
        existing.setPayloadSha256("abc");
        existing.setSignatureVerified(true);
        existing.setProcessingStatus(StripeWebhookProcessingStatus.PROCESSED);
        given(webhookEventRepository.saveAndFlush(any(StripeWebhookEventJpaEntity.class))).willThrow(new DataIntegrityViolationException("duplicate"));
        given(webhookEventRepository.findByProviderAndProviderEventId(PaymentProvider.STRIPE, "evt_dup")).willReturn(Optional.of(existing));

        StripeWebhookOutcome outcome = service.handle(payload, signature);

        assertThat(outcome.duplicate()).isTrue();
        verify(confirmationService, org.mockito.Mockito.never()).confirmAndRecord(any(String.class));
    }

    @Test
    void rejectsInvalidSignature() {
        StripeWebhookApplicationService service = new StripeWebhookApplicationService(
                properties,
                objectMapper,
                webhookEventRepository,
                confirmationService,
                auditLogService,
                outboxEventService,
                metricsRecorder
        );

        assertThatThrownBy(() -> service.handle("{\"id\":\"evt_bad\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_123\"}}}", "t=1,v1=bad"))
                .isInstanceOf(PayBridgeException.class)
                .hasMessageContaining("signature verification failed");
    }

    private PayBridgeProperties configuredProperties() {
        PayBridgeProperties props = new PayBridgeProperties();
        props.getFeatures().setStripeEnabled(true);
        props.getProviders().getStripe().setEnabled(true);
        props.getProviders().getStripe().setWebhookSigningSecret("whsec_test_paybridge");
        props.getProviders().getStripe().setPublishableKey("pk_test_paybridge");
        props.getProviders().getStripe().setSecretKey("sk_test_paybridge");
        return props;
    }

    private String validSignature(String payload, String secret) {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String digest = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
            return "t=" + timestamp + ",v1=" + digest;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
