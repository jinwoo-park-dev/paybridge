package com.paybridge.providers.nicepay;

import com.paybridge.payment.application.CreateApprovedPaymentCommand;
import com.paybridge.payment.application.IdempotencyApplicationService;
import com.paybridge.payment.application.IdempotencyDecision;
import com.paybridge.payment.application.IdempotencyReservationResult;
import com.paybridge.payment.application.PaymentCommandApplicationService;
import com.paybridge.payment.domain.IdempotencyOperation;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NicePayApprovalApplicationService {

    private final PayBridgeProperties payBridgeProperties;
    private final NicePayClient nicePayClient;
    private final NicePayCryptoSupport nicePayCryptoSupport;
    private final NicePayTidGenerator nicePayTidGenerator;
    private final IdempotencyApplicationService idempotencyApplicationService;
    private final PaymentCommandApplicationService paymentCommandApplicationService;

    public NicePayApprovalApplicationService(
            PayBridgeProperties payBridgeProperties,
            NicePayClient nicePayClient,
            NicePayCryptoSupport nicePayCryptoSupport,
            NicePayTidGenerator nicePayTidGenerator,
            IdempotencyApplicationService idempotencyApplicationService,
            PaymentCommandApplicationService paymentCommandApplicationService
    ) {
        this.payBridgeProperties = payBridgeProperties;
        this.nicePayClient = nicePayClient;
        this.nicePayCryptoSupport = nicePayCryptoSupport;
        this.nicePayTidGenerator = nicePayTidGenerator;
        this.idempotencyApplicationService = idempotencyApplicationService;
        this.paymentCommandApplicationService = paymentCommandApplicationService;
    }

    public NicePayApprovalOutcome approve(NicePayKeyInApprovalCommand command) {
        String idempotencyKey = "nicepay-approve:" + command.orderId();
        String requestHash = nicePayCryptoSupport.sha256Hex(
                command.orderId() + "|"
                        + command.amountMinor() + "|"
                        + command.goodsName() + "|"
                        + command.cardInterest() + "|"
                        + command.cardQuota() + "|"
                        + nullSafe(command.buyerName()) + "|"
                        + nullSafe(command.buyerEmail()) + "|"
                        + nullSafe(command.buyerTel())
        );

        IdempotencyReservationResult reservation = idempotencyApplicationService.reserve(
                IdempotencyOperation.CREATE_PAYMENT,
                idempotencyKey,
                requestHash
        );

        if (reservation.decision() == IdempotencyDecision.REPLAY && reservation.resultPaymentId() != null) {
            return new NicePayApprovalOutcome(reservation.resultPaymentId(), true, null, null);
        }
        if (reservation.decision() == IdempotencyDecision.CONFLICT) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "A different approval request already used the same order idempotency key.");
        }
        if (reservation.decision() == IdempotencyDecision.IN_PROGRESS) {
            throw new PayBridgeException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "An approval request for this order is already in progress.");
        }

        try {
            PayBridgeProperties.NicePay provider = payBridgeProperties.getProviders().getNicepay();
            String merchantId = provider.getMerchantId();
            String merchantKey = provider.getMerchantKey();
            String amount = Long.toString(command.amountMinor());
            String ediDate = nicePayTidGenerator.nextEdiDate();
            String tid = nicePayTidGenerator.nextCreditCardTid(merchantId);
            String encData = nicePayCryptoSupport.encryptEncData(command.cardDetails(), merchantKey);
            String signData = nicePayCryptoSupport.generateApprovalSignData(merchantId, amount, ediDate, command.orderId(), merchantKey);

            NicePayApprovalRequest request = new NicePayApprovalRequest(
                    tid,
                    merchantId,
                    ediDate,
                    command.orderId(),
                    amount,
                    command.goodsName(),
                    encData,
                    signData,
                    command.cardInterest(),
                    command.cardQuota(),
                    command.buyerName(),
                    command.buyerEmail(),
                    command.buyerTel(),
                    StandardCharsets.UTF_8.name().toLowerCase(),
                    "KV"
            );

            NicePayApprovalResponse response = nicePayClient.approve(request);
            UUID paymentId = paymentCommandApplicationService.recordApprovedPayment(
                    new CreateApprovedPaymentCommand(
                            command.orderId(),
                            PaymentProvider.NICEPAY,
                            response.tid(),
                            response.authCode().isBlank() ? response.tid() : response.authCode(),
                            response.amountMinor(),
                            response.partialCancelSupported(),
                            "KRW",
                            null
                    )
            );
            idempotencyApplicationService.markCompleted(reservation.idempotencyRecordId(), paymentId);
            return new NicePayApprovalOutcome(paymentId, false, response.tid(), response.authCode());
        } catch (RuntimeException ex) {
            idempotencyApplicationService.release(reservation.idempotencyRecordId());
            throw ex;
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }
}
