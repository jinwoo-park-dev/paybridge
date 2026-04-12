package com.paybridge.payment.application;

import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import java.time.Instant;

public record TransactionExportCriteria(
        Instant approvedFrom,
        Instant approvedTo,
        PaymentProvider provider,
        PaymentStatus status
) {
}
