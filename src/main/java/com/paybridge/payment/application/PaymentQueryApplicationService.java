package com.paybridge.payment.application;

import com.paybridge.payment.domain.Payment;
import com.paybridge.payment.persistence.PaymentJpaEntity;
import com.paybridge.payment.persistence.PaymentJpaRepository;
import com.paybridge.payment.persistence.PaymentPersistenceMapper;
import com.paybridge.payment.persistence.PaymentReversalJpaEntity;
import com.paybridge.payment.persistence.PaymentReversalJpaRepository;
import com.paybridge.payment.persistence.PaymentSpecifications;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaymentQueryApplicationService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Sort EXPORT_SORT = Sort.by(
        Sort.Order.desc("approvedAt"),
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );

    private final PaymentJpaRepository paymentRepository;
    private final PaymentReversalJpaRepository paymentReversalRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;

    public PaymentQueryApplicationService(
        PaymentJpaRepository paymentRepository,
        PaymentReversalJpaRepository paymentReversalRepository,
        PaymentPersistenceMapper paymentPersistenceMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentReversalRepository = paymentReversalRepository;
        this.paymentPersistenceMapper = paymentPersistenceMapper;
    }

    public List<TransactionSummaryView> search(TransactionSearchCriteria criteria) {
        return paymentRepository.findAll(PaymentSpecifications.from(criteria), DEFAULT_SORT)
            .stream()
            .limit(100)
            .map(this::toSummaryView)
            .toList();
    }

    public TransactionExportPageView export(TransactionExportCriteria criteria, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, EXPORT_SORT);
        Page<PaymentJpaEntity> exportPage = paymentRepository.findAll(PaymentSpecifications.from(criteria), pageable);

        return new TransactionExportPageView(
            exportPage.getContent().stream().map(this::toExportView).toList(),
            exportPage.getNumber(),
            exportPage.getSize(),
            exportPage.hasNext()
        );
    }

    public PaymentDetailView getDetail(UUID paymentId) {
        PaymentJpaEntity paymentJpaEntity = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PayBridgeException(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                "Payment not found: " + paymentId
            ));

        Payment payment = paymentPersistenceMapper.toDomain(paymentJpaEntity);
        List<PaymentReversalView> reversals = paymentReversalRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId)
            .stream()
            .map(reversal -> toReversalView(payment.currency(), reversal))
            .toList();

        return new PaymentDetailView(
            payment.paymentId(),
            payment.orderId(),
            payment.provider(),
            payment.status(),
            MoneyDisplayFormatter.formatMinor(payment.currency(), payment.amountMinor()),
            MoneyDisplayFormatter.formatMinor(payment.currency(), payment.reversibleAmountMinor()),
            payment.currency(),
            nullToDash(payment.providerPaymentId()),
            nullToDash(payment.providerTransactionId()),
            TimeDisplayFormatter.format(payment.approvedAt()),
            payment.allowsFullReversal(),
            payment.allowsPartialReversal(),
            reversals
        );
    }

    private TransactionSummaryView toSummaryView(PaymentJpaEntity entity) {
        Payment payment = paymentPersistenceMapper.toDomain(entity);
        return new TransactionSummaryView(
            payment.paymentId(),
            payment.orderId(),
            payment.provider(),
            payment.status(),
            MoneyDisplayFormatter.formatMinor(payment.currency(), payment.amountMinor()),
            MoneyDisplayFormatter.formatMinor(payment.currency(), payment.reversibleAmountMinor()),
            nullToDash(payment.providerPaymentId()),
            nullToDash(payment.providerTransactionId()),
            TimeDisplayFormatter.format(payment.approvedAt())
        );
    }

    private TransactionExportView toExportView(PaymentJpaEntity entity) {
        Payment payment = paymentPersistenceMapper.toDomain(entity);
        return new TransactionExportView(
            payment.paymentId(),
            payment.orderId(),
            payment.provider(),
            payment.status(),
            payment.amountMinor(),
            payment.reversibleAmountMinor(),
            payment.currency(),
            payment.providerPaymentId(),
            payment.providerTransactionId(),
            payment.approvedAt(),
            payment.createdAt(),
            payment.updatedAt()
        );
    }

    private PaymentReversalView toReversalView(String currency, PaymentReversalJpaEntity entity) {
        return new PaymentReversalView(
            entity.getId(),
            entity.getReversalType(),
            entity.getStatus(),
            MoneyDisplayFormatter.formatMinor(currency, entity.getAmountMinor()),
            MoneyDisplayFormatter.formatMinor(currency, entity.getRemainingAmountMinor()),
            entity.getReason(),
            nullToDash(entity.getProviderReversalId()),
            TimeDisplayFormatter.format(entity.getProcessedAt().toInstant())
        );
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
