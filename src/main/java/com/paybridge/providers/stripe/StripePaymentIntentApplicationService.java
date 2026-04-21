package com.paybridge.providers.stripe;

import com.paybridge.payment.application.CreateApprovedPaymentCommand;
import com.paybridge.payment.application.IdempotencyApplicationService;
import com.paybridge.payment.application.IdempotencyDecision;
import com.paybridge.payment.application.IdempotencyReservationResult;
import com.paybridge.payment.application.MoneyDisplayFormatter;
import com.paybridge.payment.application.PaymentCommandApplicationService;
import com.paybridge.payment.domain.IdempotencyOperation;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import com.paybridge.support.logging.SensitiveValueMasker;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentIntentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentIntentApplicationService.class);

    private final StripeApiClient stripeApiClient;
    private final IdempotencyApplicationService idempotencyApplicationService;
    private final PaymentCommandApplicationService paymentCommandApplicationService;

    public StripePaymentIntentApplicationService(
            StripeApiClient stripeApiClient,
            IdempotencyApplicationService idempotencyApplicationService,
            PaymentCommandApplicationService paymentCommandApplicationService
    ) {
        this.stripeApiClient = stripeApiClient;
        this.idempotencyApplicationService = idempotencyApplicationService;
        this.paymentCommandApplicationService = paymentCommandApplicationService;
    }

    public StripeCheckoutPageView createCheckoutSession(StripeCreatePaymentIntentCommand command) {
        validate(command);
        StripeCreatePaymentIntentRequest request = new StripeCreatePaymentIntentRequest(
                command.orderId().trim(),
                command.amountMinor(),
                command.currency().trim(),
                command.description(),
                command.customerEmail(),
                "paybridge-stripe-intent-" + command.orderId().trim()
        );
        StripePaymentIntentResponse response = stripeApiClient.createPaymentIntent(request);
        boolean confirmationRequired = isBrowserConfirmationRequired(response.status());
        String clientSecret = confirmationRequired
                ? requireText(response.clientSecret(), "Stripe PaymentIntent client_secret")
                : nullSafe(response.clientSecret());
        return new StripeCheckoutPageView(
                response.id(),
                clientSecret,
                command.orderId().trim(),
                MoneyDisplayFormatter.formatMinor(response.currency(), response.amountMinor()),
                response.currency(),
                command.description(),
                response.status(),
                confirmationRequired
        );
    }

    public StripePaymentConfirmationOutcome confirmAndRecord(String paymentIntentId) {
        String normalizedPaymentIntentId = requireText(paymentIntentId, "paymentIntentId");
        StripePaymentIntentResponse paymentIntent = stripeApiClient.retrievePaymentIntent(normalizedPaymentIntentId);
        if (!"succeeded".equalsIgnoreCase(paymentIntent.status())) {
            throw new PayBridgeException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Stripe payment is not in succeeded status yet. Current status: " + paymentIntent.status()
            );
        }
        String orderId = requireText(paymentIntent.orderId(), "Stripe PaymentIntent metadata.order_id");
        String requestHash = sha256Hex(
                paymentIntent.id() + "|" + orderId + "|" + paymentIntent.amountMinor() + "|"
                        + paymentIntent.currency() + "|" + nullSafe(paymentIntent.latestChargeId())
        );
        IdempotencyReservationResult reservation = idempotencyApplicationService.reserve(
                IdempotencyOperation.CREATE_PAYMENT,
                "stripe-approve:" + paymentIntent.id(),
                requestHash
        );

        if (reservation.decision() == IdempotencyDecision.REPLAY && reservation.resultPaymentId() != null) {
            return confirmationOutcome(reservation.resultPaymentId(), true, paymentIntent, orderId);
        }
        if (reservation.decision() == IdempotencyDecision.CONFLICT) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "A different Stripe confirmation request already exists for this PaymentIntent.");
        }
        if (reservation.decision() == IdempotencyDecision.IN_PROGRESS) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "This Stripe confirmation is already in progress.");
        }

        try {
            UUID paymentId = paymentCommandApplicationService.recordApprovedPayment(
                    new CreateApprovedPaymentCommand(
                            orderId,
                            PaymentProvider.STRIPE,
                            paymentIntent.id(),
                            paymentIntent.latestChargeId(),
                            paymentIntent.amountMinor(),
                            true,
                            paymentIntent.currency(),
                            Instant.now()
                    )
            );
            idempotencyApplicationService.markCompleted(reservation.idempotencyRecordId(), paymentId);
            log.info(
                    "Stripe payment confirmed and recorded: paymentIntentId={}, paymentId={}, latestChargeId={}",
                    SensitiveValueMasker.maskProviderIdentifier(paymentIntent.id()),
                    paymentId,
                    SensitiveValueMasker.maskProviderIdentifier(paymentIntent.latestChargeId())
            );
            return confirmationOutcome(paymentId, false, paymentIntent, orderId);
        } catch (RuntimeException ex) {
            idempotencyApplicationService.release(reservation.idempotencyRecordId());
            throw ex;
        }
    }

    private StripePaymentConfirmationOutcome confirmationOutcome(
            UUID paymentId,
            boolean replayed,
            StripePaymentIntentResponse paymentIntent,
            String orderId
    ) {
        return new StripePaymentConfirmationOutcome(
                paymentId,
                replayed,
                paymentIntent.id(),
                paymentIntent.status(),
                orderId,
                MoneyDisplayFormatter.formatMinor(paymentIntent.currency(), paymentIntent.amountMinor()),
                paymentIntent.currency(),
                paymentIntent.latestChargeId()
        );
    }

    private boolean isBrowserConfirmationRequired(String status) {
        if (status == null) {
            return false;
        }
        return switch (status.toLowerCase()) {
            case "requires_payment_method", "requires_confirmation", "requires_action" -> true;
            default -> false;
        };
    }

    private void validate(StripeCreatePaymentIntentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireText(command.orderId(), "orderId");
        requireText(command.currency(), "currency");
        if (command.amountMinor() <= 0) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Stripe amount must be positive.");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
