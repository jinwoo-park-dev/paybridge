package com.paybridge.providers.stripe.webhook;

import com.paybridge.support.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stripe Webhooks", description = "Verified Stripe webhook intake with duplicate suppression and outbox/audit side-effects.")
@RestController
public class StripeWebhookController {

    private final StripeWebhookApplicationService stripeWebhookApplicationService;

    public StripeWebhookController(StripeWebhookApplicationService stripeWebhookApplicationService) {
        this.stripeWebhookApplicationService = stripeWebhookApplicationService;
    }

    @Operation(
            summary = "Receive a Stripe webhook event",
            description = "Verifies the Stripe signature, suppresses duplicates, records audit/outbox state, and returns a small acknowledgement payload.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Webhook accepted and processed or marked duplicate.",
                            content = @Content(schema = @Schema(implementation = StripeWebhookAcknowledgementResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload or Stripe signature verification failed.",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Stripe webhook handling is not configured for the current environment.",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PostMapping(path = "/api/providers/stripe/webhooks", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StripeWebhookAcknowledgementResponse handle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Raw Stripe event payload as JSON string.")
            @org.springframework.web.bind.annotation.RequestBody String payload,
            @Parameter(description = "Stripe generated signature header", required = true)
            @RequestHeader(name = "Stripe-Signature") String signatureHeader
    ) {
        StripeWebhookOutcome outcome = stripeWebhookApplicationService.handle(payload, signatureHeader);
        return new StripeWebhookAcknowledgementResponse(
                true,
                outcome.duplicate(),
                outcome.eventId(),
                outcome.eventType(),
                outcome.processingStatus().name()
        );
    }
}
