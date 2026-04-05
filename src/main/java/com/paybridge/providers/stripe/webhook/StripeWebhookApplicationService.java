package com.paybridge.providers.stripe.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.audit.AuditAction;
import com.paybridge.audit.AuditLogService;
import com.paybridge.ops.outbox.OutboxEventService;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.providers.stripe.StripePaymentConfirmationOutcome;
import com.paybridge.providers.stripe.StripePaymentIntentApplicationService;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import com.paybridge.support.logging.SensitiveValueMasker;
import com.paybridge.support.metrics.PaymentMetricsRecorder;
import com.paybridge.support.web.RequestCorrelationFilter;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookApplicationService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookApplicationService.class);

    private final PayBridgeProperties payBridgeProperties;
    private final ObjectMapper objectMapper;
    private final StripeWebhookEventJpaRepository webhookEventRepository;
    private final StripePaymentIntentApplicationService stripePaymentIntentApplicationService;
    private final AuditLogService auditLogService;
    private final OutboxEventService outboxEventService;
    private final PaymentMetricsRecorder paymentMetricsRecorder;

    public StripeWebhookApplicationService(
            PayBridgeProperties payBridgeProperties,
            ObjectMapper objectMapper,
            StripeWebhookEventJpaRepository webhookEventRepository,
            StripePaymentIntentApplicationService stripePaymentIntentApplicationService,
            AuditLogService auditLogService,
            OutboxEventService outboxEventService,
            PaymentMetricsRecorder paymentMetricsRecorder
    ) {
        this.payBridgeProperties = payBridgeProperties;
        this.objectMapper = objectMapper;
        this.webhookEventRepository = webhookEventRepository;
        this.stripePaymentIntentApplicationService = stripePaymentIntentApplicationService;
        this.auditLogService = auditLogService;
        this.outboxEventService = outboxEventService;
        this.paymentMetricsRecorder = paymentMetricsRecorder;
    }

    public StripeWebhookOutcome handle(String payload, String signatureHeader) {
        requireConfiguredProvider();
        String normalizedPayload = requireText(payload, "Stripe webhook payload");
        String normalizedSignature = requireText(signatureHeader, "Stripe-Signature header");
        Event event = verifySignature(normalizedPayload, normalizedSignature);
        String eventId = requireText(event.getId(), "Stripe event.id");
        String eventType = requireText(event.getType(), "Stripe event.type");

        paymentMetricsRecorder.recordWebhookReceived(PaymentProvider.STRIPE, eventType);
        log.info(
                "Stripe webhook received: eventId={}, eventType={}, signature={}",
                eventId,
                eventType,
                SensitiveValueMasker.maskSignatureHeader(normalizedSignature)
        );

        StripeWebhookEventJpaEntity entity = new StripeWebhookEventJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setProvider(PaymentProvider.STRIPE);
        entity.setProviderEventId(eventId);
        entity.setEventType(eventType);
        entity.setPayloadSha256(sha256Hex(normalizedPayload));
        entity.setSignatureVerified(true);
        entity.setProcessingStatus(StripeWebhookProcessingStatus.RECEIVED);
        entity.setCorrelationId(MDC.get(RequestCorrelationFilter.CORRELATION_ID_MDC_KEY));

        try {
            entity = webhookEventRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            return duplicateOutcome(eventId, eventType);
        }

        auditLogService.info(
                AuditAction.STRIPE_WEBHOOK_RECEIVED,
                "stripe_webhook",
                eventId,
                PaymentProvider.STRIPE,
                "stripe",
                "Stripe webhook accepted after signature verification.",
                Map.of("eventType", eventType)
        );

        try {
            processEvent(eventType, normalizedPayload, eventId);
            entity.setProcessingStatus(StripeWebhookProcessingStatus.PROCESSED);
            entity.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            webhookEventRepository.saveAndFlush(entity);
            auditLogService.success(
                    AuditAction.STRIPE_WEBHOOK_PROCESSED,
                    "stripe_webhook",
                    eventId,
                    PaymentProvider.STRIPE,
                    "stripe",
                    "Stripe webhook processed successfully.",
                    Map.of("eventType", eventType)
            );
            outboxEventService.appendStripeWebhookAcknowledged(eventId, eventType, StripeWebhookProcessingStatus.PROCESSED.name());
            paymentMetricsRecorder.recordOutboxAppended("stripe_webhook_acknowledged");
            return new StripeWebhookOutcome(eventId, eventType, false, StripeWebhookProcessingStatus.PROCESSED);
        } catch (RuntimeException ex) {
            entity.setProcessingStatus(StripeWebhookProcessingStatus.FAILED);
            entity.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            entity.setLastError(limit(ex.getMessage()));
            webhookEventRepository.saveAndFlush(entity);
            auditLogService.failure(
                    AuditAction.STRIPE_WEBHOOK_REJECTED,
                    "stripe_webhook",
                    eventId,
                    PaymentProvider.STRIPE,
                    "stripe",
                    "Stripe webhook processing failed.",
                    Map.of("eventType", eventType, "reason", safeValue(limit(ex.getMessage())))
            );
            throw ex;
        }
    }

    private StripeWebhookOutcome duplicateOutcome(String eventId, String eventType) {
        StripeWebhookProcessingStatus existingStatus = webhookEventRepository
                .findByProviderAndProviderEventId(PaymentProvider.STRIPE, eventId)
                .map(StripeWebhookEventJpaEntity::getProcessingStatus)
                .orElse(StripeWebhookProcessingStatus.DUPLICATE);

        paymentMetricsRecorder.recordWebhookDuplicate(PaymentProvider.STRIPE, eventType);
        auditLogService.info(
                AuditAction.STRIPE_WEBHOOK_DUPLICATE,
                "stripe_webhook",
                eventId,
                PaymentProvider.STRIPE,
                "stripe",
                "Duplicate Stripe webhook ignored.",
                Map.of("eventType", eventType, "existingStatus", existingStatus.name())
        );
        return new StripeWebhookOutcome(eventId, eventType, true, StripeWebhookProcessingStatus.DUPLICATE);
    }

    private void processEvent(String eventType, String payload, String eventId) {
        JsonNode root = readJson(payload);
        if ("payment_intent.succeeded".equals(eventType)) {
            String paymentIntentId = requireText(root.path("data").path("object").path("id").asText(null), "Stripe payment_intent id");
            StripePaymentConfirmationOutcome outcome = stripePaymentIntentApplicationService.confirmAndRecord(paymentIntentId);
            log.info(
                    "Stripe webhook processed payment_intent.succeeded: eventId={}, paymentIntentId={}, paymentId={}, replayed={}",
                    eventId,
                    SensitiveValueMasker.maskProviderIdentifier(paymentIntentId),
                    outcome.paymentId(),
                    outcome.replayed()
            );
            return;
        }

        if ("charge.refunded".equals(eventType) || "refund.updated".equals(eventType)) {
            log.info("Stripe webhook acknowledged without domain mutation: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        log.info("Stripe webhook type currently treated as audit-only: eventId={}, eventType={}", eventId, eventType);
    }

    private Event verifySignature(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, payBridgeProperties.getProviders().getStripe().getWebhookSigningSecret());
        } catch (SignatureVerificationException ex) {
            paymentMetricsRecorder.recordWebhookRejected(PaymentProvider.STRIPE);
            auditLogService.failure(
                    AuditAction.STRIPE_WEBHOOK_REJECTED,
                    "stripe_webhook",
                    null,
                    PaymentProvider.STRIPE,
                    "stripe",
                    "Stripe webhook signature verification failed.",
                    Map.of("reason", safeValue(limit(ex.getMessage())))
            );
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Stripe webhook signature verification failed.");
        } catch (RuntimeException ex) {
            paymentMetricsRecorder.recordWebhookRejected(PaymentProvider.STRIPE);
            throw ex;
        }
    }

    private void requireConfiguredProvider() {
        boolean featureEnabled = payBridgeProperties.getFeatures().isStripeEnabled();
        PayBridgeProperties.Stripe provider = payBridgeProperties.getProviders().getStripe();
        if (!featureEnabled || !provider.isEnabled()) {
            throw new PayBridgeException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.PROVIDER_ERROR,
                    "Stripe integration is disabled. Enable paybridge.features.stripe-enabled and paybridge.providers.stripe.enabled first."
            );
        }
        if (provider.getWebhookSigningSecret() == null || provider.getWebhookSigningSecret().isBlank()) {
            throw new PayBridgeException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.PROVIDER_ERROR,
                    "Stripe webhook signing secret is missing. Provide PAYBRIDGE_STRIPE_WEBHOOK_SECRET before local webhook verification."
            );
        }
    }

    private JsonNode readJson(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Stripe webhook payload is not valid JSON.");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private String safeValue(String value) {
        return value == null ? "(not provided)" : value;
    }
}
