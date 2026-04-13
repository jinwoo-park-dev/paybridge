package com.paybridge.operator.api;

import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.application.TransactionExportCriteria;
import com.paybridge.payment.application.TransactionExportPageView;
import com.paybridge.payment.application.TransactionSearchCriteria;
import com.paybridge.payment.application.TransactionSummaryView;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Operator Transactions API", description = "Read-only transaction search, detail, and export endpoints for operator tooling.")
@RestController
@RequestMapping("/api/ops/transactions")
public class OperatorTransactionsApiController {

    private static final int DEFAULT_EXPORT_PAGE = 0;
    private static final int DEFAULT_EXPORT_SIZE = 200;
    private static final int MAX_EXPORT_SIZE = 500;

    private final PaymentQueryApplicationService paymentQueryApplicationService;

    public OperatorTransactionsApiController(PaymentQueryApplicationService paymentQueryApplicationService) {
        this.paymentQueryApplicationService = paymentQueryApplicationService;
    }

    @Operation(summary = "Search transactions")
    @GetMapping
    public List<TransactionSummaryView> search(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String providerPaymentId,
            @RequestParam(required = false) String providerTransactionId,
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) PaymentStatus status
    ) {
        return paymentQueryApplicationService.search(
                new TransactionSearchCriteria(
                        orderId,
                        providerPaymentId,
                        providerTransactionId,
                        provider,
                        status
                )
        );
    }

    @Operation(summary = "Export machine-readable transaction snapshots")
    @GetMapping("/export")
    public TransactionExportPageView export(
            @Parameter(description = "Inclusive lower bound for approvedAt in ISO-8601 UTC format.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant approvedFrom,
            @Parameter(description = "Exclusive upper bound for approvedAt in ISO-8601 UTC format.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant approvedTo,
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return paymentQueryApplicationService.export(
                new TransactionExportCriteria(
                        approvedFrom,
                        approvedTo,
                        provider,
                        status
                ),
                validatedPage(page),
                validatedSize(size)
        );
    }

    @Operation(summary = "Get a transaction detail")
    @GetMapping("/{paymentId}")
    public PaymentDetailView detail(@PathVariable UUID paymentId) {
        return paymentQueryApplicationService.getDetail(paymentId);
    }

    private int validatedPage(Integer page) {
        int resolvedPage = page == null ? DEFAULT_EXPORT_PAGE : page;
        if (resolvedPage < 0) {
            throw new PayBridgeException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_ERROR,
                    "page must be greater than or equal to 0."
            );
        }
        return resolvedPage;
    }

    private int validatedSize(Integer size) {
        int resolvedSize = size == null ? DEFAULT_EXPORT_SIZE : size;
        if (resolvedSize < 1 || resolvedSize > MAX_EXPORT_SIZE) {
            throw new PayBridgeException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_ERROR,
                    "size must be between 1 and 500."
            );
        }
        return resolvedSize;
    }
}
