package com.paybridge.providers.stripe;

import com.paybridge.payment.application.IdempotencyApplicationService;
import com.paybridge.payment.application.IdempotencyDecision;
import com.paybridge.payment.application.IdempotencyReservationResult;
import com.paybridge.payment.application.PaymentCommandApplicationService;
import com.paybridge.payment.application.RegisterFullReversalCommand;
import com.paybridge.payment.application.RegisterPartialReversalCommand;
import com.paybridge.payment.domain.IdempotencyOperation;
import com.paybridge.payment.domain.Payment;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.persistence.PaymentJpaEntity;
import com.paybridge.payment.persistence.PaymentJpaRepository;
import com.paybridge.payment.persistence.PaymentPersistenceMapper;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class StripeRefundApplicationService {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;
    private final PaymentCommandApplicationService paymentCommandApplicationService;
    private final IdempotencyApplicationService idempotencyApplicationService;
    private final StripeApiClient stripeApiClient;

    public StripeRefundApplicationService(
            PaymentJpaRepository paymentJpaRepository,
            PaymentPersistenceMapper paymentPersistenceMapper,
            PaymentCommandApplicationService paymentCommandApplicationService,
            IdempotencyApplicationService idempotencyApplicationService,
            StripeApiClient stripeApiClient
    ) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentPersistenceMapper = paymentPersistenceMapper;
        this.paymentCommandApplicationService = paymentCommandApplicationService;
        this.idempotencyApplicationService = idempotencyApplicationService;
        this.stripeApiClient = stripeApiClient;
    }

    public StripeRefundOutcome refundFully(UUID paymentId, String reason) {
        Payment payment = loadStripePayment(paymentId);
        return executeRefund(payment, payment.reversibleAmountMinor(), false, normalizeReason(reason, "Customer requested full refund."));
    }

    public StripeRefundOutcome refundPartially(UUID paymentId, long amountMinor, String reason) {
        Payment payment = loadStripePayment(paymentId);
        if (amountMinor <= 0) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Partial refund amount must be positive.");
        }
        if (amountMinor >= payment.reversibleAmountMinor()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Partial refund amount must be smaller than the remaining reversible amount.");
        }
        return executeRefund(payment, amountMinor, true, normalizeReason(reason, "Customer requested partial refund."));
    }

    private StripeRefundOutcome executeRefund(Payment payment, long refundAmountMinor, boolean partial, String reason) {
        if (partial && !payment.allowsPartialReversal()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "This payment does not allow partial refunds.");
        }
        if (!partial && !payment.allowsFullReversal()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "This payment is no longer eligible for a full refund.");
        }
        String idempotencyKey = "stripe-refund:" + payment.paymentId() + ":" + (partial ? "PARTIAL" : "FULL") + ":" + refundAmountMinor;
        String requestHash = sha256Hex(payment.providerPaymentId() + "|" + nullSafe(payment.providerTransactionId()) + "|" + refundAmountMinor + "|" + reason);

        IdempotencyOperation operation = partial
                ? IdempotencyOperation.PARTIAL_REVERSAL
                : IdempotencyOperation.FULL_REVERSAL;

        IdempotencyReservationResult reservation = idempotencyApplicationService.reserve(
                operation,
                idempotencyKey,
                requestHash
        );

        if (reservation.decision() == IdempotencyDecision.REPLAY && reservation.resultPaymentId() != null) {
            return new StripeRefundOutcome(reservation.resultPaymentId(), null, partial, null);
        }
        if (reservation.decision() == IdempotencyDecision.CONFLICT) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "A different Stripe refund request already used the same idempotency key.");
        }
        if (reservation.decision() == IdempotencyDecision.IN_PROGRESS) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "This Stripe refund request is already in progress.");
        }

        try {
            StripeRefundResponse refund = stripeApiClient.createRefund(
                    new StripeRefundRequest(
                            payment.providerPaymentId(),
                            payment.providerTransactionId(),
                            partial ? refundAmountMinor : null,
                            reason,
                            idempotencyKey
                    )
            );
            if (!refund.isSucceeded()) {
                throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe refund completed with unsupported status: " + refund.status());
            }
            UUID reversalId = partial
                    ? paymentCommandApplicationService.recordPartialReversal(
                            new RegisterPartialReversalCommand(payment.paymentId(), refund.amountMinor(), reason, refund.id(), Instant.now())
                    )
                    : paymentCommandApplicationService.recordFullReversal(
                            new RegisterFullReversalCommand(payment.paymentId(), reason, refund.id(), Instant.now())
                    );
            idempotencyApplicationService.markCompleted(reservation.idempotencyRecordId(), payment.paymentId());
            return new StripeRefundOutcome(payment.paymentId(), reversalId, partial, refund.id());
        } catch (RuntimeException ex) {
            idempotencyApplicationService.release(reservation.idempotencyRecordId());
            throw ex;
        }
    }

    private Payment loadStripePayment(UUID paymentId) {
        PaymentJpaEntity entity = paymentJpaRepository.findById(paymentId)
                .orElseThrow(() -> new PayBridgeException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Payment not found: " + paymentId));
        Payment payment = paymentPersistenceMapper.toDomain(entity);
        if (payment.provider() != PaymentProvider.STRIPE) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Only Stripe transactions can be refunded from this endpoint.");
        }
        return payment;
    }

    private String normalizeReason(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
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
