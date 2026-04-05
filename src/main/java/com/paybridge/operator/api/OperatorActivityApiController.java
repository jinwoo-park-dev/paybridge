package com.paybridge.operator.api;

import com.paybridge.audit.AuditLogQueryService;
import com.paybridge.audit.AuditLogView;
import com.paybridge.ops.outbox.OutboxEventQueryService;
import com.paybridge.ops.outbox.OutboxEventView;
import com.paybridge.providers.stripe.webhook.StripeWebhookEventView;
import com.paybridge.providers.stripe.webhook.StripeWebhookQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Operator Activity API", description = "Read-only activity endpoints for audit, outbox, and webhook inspection.")
@RestController
@RequestMapping("/api/ops")
public class OperatorActivityApiController {

    private final AuditLogQueryService auditLogQueryService;
    private final OutboxEventQueryService outboxEventQueryService;
    private final StripeWebhookQueryService stripeWebhookQueryService;

    public OperatorActivityApiController(
            AuditLogQueryService auditLogQueryService,
            OutboxEventQueryService outboxEventQueryService,
            StripeWebhookQueryService stripeWebhookQueryService
    ) {
        this.auditLogQueryService = auditLogQueryService;
        this.outboxEventQueryService = outboxEventQueryService;
        this.stripeWebhookQueryService = stripeWebhookQueryService;
    }

    @Operation(summary = "Get audit log entries for a payment")
    @GetMapping("/transactions/{paymentId}/audit-logs")
    public List<AuditLogView> auditLogs(@PathVariable UUID paymentId) {
        return auditLogQueryService.findByPaymentId(paymentId);
    }

    @Operation(summary = "Get outbox events for a payment")
    @GetMapping("/transactions/{paymentId}/outbox-events")
    public List<OutboxEventView> outboxEvents(@PathVariable UUID paymentId) {
        return outboxEventQueryService.findByPaymentId(paymentId);
    }

    @Operation(summary = "Get recent Stripe webhook events")
    @GetMapping("/stripe-webhooks")
    public List<StripeWebhookEventView> stripeWebhooks() {
        return stripeWebhookQueryService.findRecent();
    }
}
