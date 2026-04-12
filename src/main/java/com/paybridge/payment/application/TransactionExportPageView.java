package com.paybridge.payment.application;

import java.util.List;

public record TransactionExportPageView(
        List<TransactionExportView> content,
        int page,
        int size,
        boolean hasNext
) {
}
