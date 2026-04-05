package com.paybridge.providers.nicepay;

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
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NicePayCancellationApplicationService {

    private final PayBridgeProperties payBridgeProperties;
    private final NicePayClient nicePayClient;
    private final NicePayCryptoSupport nicePayCryptoSupport;
    private final NicePayTidGenerator nicePayTidGenerator;
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;
    private final PaymentCommandApplicationService paymentCommandApplicationService;
    private final IdempotencyApplicationService idempotencyApplicationService;

    public NicePayCancellationApplicationService(
            PayBridgeProperties payBridgeProperties,
            NicePayClient nicePayClient,
            NicePayCryptoSupport nicePayCryptoSupport,
            NicePayTidGenerator nicePayTidGenerator,
            PaymentJpaRepository paymentJpaRepository,
            PaymentPersistenceMapper paymentPersistenceMapper,
            PaymentCommandApplicationService paymentCommandApplicationService,
            IdempotencyApplicationService idempotencyApplicationService
    ) {
        this.payBridgeProperties = payBridgeProperties;
        this.nicePayClient = nicePayClient;
        this.nicePayCryptoSupport = nicePayCryptoSupport;
        this.nicePayTidGenerator = nicePayTidGenerator;
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentPersistenceMapper = paymentPersistenceMapper;
        this.paymentCommandApplicationService = paymentCommandApplicationService;
        this.idempotencyApplicationService = idempotencyApplicationService;
    }

    public NicePayCancellationOutcome cancelFully(UUID paymentId, String reason) {
        Payment payment = loadNicePayPayment(paymentId);
        String cancelAmount = Long.toString(payment.reversibleAmountMinor());
        return executeCancellation(payment, cancelAmount, false, normalizeReason(reason, "Full cancellation requested from PayBridge."));
    }

    public NicePayCancellationOutcome cancelPartially(UUID paymentId, long cancelAmountMinor, String reason) {
        Payment payment = loadNicePayPayment(paymentId);
        if (cancelAmountMinor <= 0) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Partial cancel amount must be positive.");
        }
        return executeCancellation(payment, Long.toString(cancelAmountMinor), true, normalizeReason(reason, "Partial cancellation requested from PayBridge."));
    }

    private NicePayCancellationOutcome executeCancellation(Payment payment, String cancelAmount, boolean partial, String reason) {
        if (partial && !payment.allowsPartialReversal()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "This payment does not allow NicePay partial cancellation.");
        }
        if (!partial && !payment.allowsFullReversal()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "This payment is no longer eligible for full cancellation.");
        }
        long cancelAmountMinor = Long.parseLong(cancelAmount);
        if (partial && cancelAmountMinor >= payment.reversibleAmountMinor()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Partial cancellation must be smaller than the remaining reversible amount.");
        }
        if (!partial && cancelAmountMinor != payment.reversibleAmountMinor()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Full cancellation must use the full remaining reversible amount.");
        }

        String idempotencyKey = "nicepay-cancel:" + payment.paymentId() + ":" + (partial ? "PARTIAL" : "FULL") + ":" + cancelAmount;
        String requestHash = nicePayCryptoSupport.sha256Hex(payment.providerPaymentId() + "|" + cancelAmount + "|" + partial + "|" + reason);

        IdempotencyOperation operation = partial
                ? IdempotencyOperation.PARTIAL_REVERSAL
                : IdempotencyOperation.FULL_REVERSAL;

        IdempotencyReservationResult reservation = idempotencyApplicationService.reserve(
                operation,
                idempotencyKey,
                requestHash
        );

        if (reservation.decision() == IdempotencyDecision.REPLAY && reservation.resultPaymentId() != null) {
            return new NicePayCancellationOutcome(reservation.resultPaymentId(), null, partial);
        }
        if (reservation.decision() == IdempotencyDecision.CONFLICT) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "A different cancellation request already used the same idempotency key.");
        }
        if (reservation.decision() == IdempotencyDecision.IN_PROGRESS) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "This cancellation request is already in progress.");
        }

        try {
            PayBridgeProperties.NicePay provider = payBridgeProperties.getProviders().getNicepay();
            String ediDate = nicePayTidGenerator.nextEdiDate();
            NicePayCancelRequest request = new NicePayCancelRequest(
                    required(payment.providerPaymentId(), "NicePay TID is missing on the stored payment."),
                    required(provider.getMerchantId(), "NicePay MID is not configured."),
                    payment.orderId(),
                    cancelAmount,
                    reason,
                    partial ? "1" : "0",
                    ediDate,
                    nicePayCryptoSupport.generateCancelSignData(provider.getMerchantId(), cancelAmount, ediDate, provider.getMerchantKey()),
                    StandardCharsets.UTF_8.name().toLowerCase(),
                    "KV"
            );

            NicePayCancelResponse response = nicePayClient.cancel(request);
            UUID reversalId = partial
                    ? paymentCommandApplicationService.recordPartialReversal(
                            new RegisterPartialReversalCommand(
                                    payment.paymentId(),
                                    response.cancelAmountMinor(),
                                    reason,
                                    response.cancelNumber().isBlank() ? response.tid() : response.cancelNumber(),
                                    null
                            )
                    )
                    : paymentCommandApplicationService.recordFullReversal(
                            new RegisterFullReversalCommand(
                                    payment.paymentId(),
                                    reason,
                                    response.cancelNumber().isBlank() ? response.tid() : response.cancelNumber(),
                                    null
                            )
                    );

            idempotencyApplicationService.markCompleted(reservation.idempotencyRecordId(), payment.paymentId());
            return new NicePayCancellationOutcome(payment.paymentId(), reversalId, partial);
        } catch (RuntimeException ex) {
            idempotencyApplicationService.release(reservation.idempotencyRecordId());
            throw ex;
        }
    }

    private Payment loadNicePayPayment(UUID paymentId) {
        PaymentJpaEntity entity = paymentJpaRepository.findById(paymentId)
                .orElseThrow(() -> new PayBridgeException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Payment not found: " + paymentId));
        Payment payment = paymentPersistenceMapper.toDomain(entity);
        if (payment.provider() != PaymentProvider.NICEPAY) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Only NicePay transactions can be cancelled from this endpoint.");
        }
        return payment;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PayBridgeException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.PROVIDER_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeReason(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
