package com.paybridge.operator.api;

import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.application.TransactionSearchCriteria;
import com.paybridge.payment.application.TransactionSummaryView;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Operator Transactions API", description = "Read-only transaction search and detail endpoints for operator tooling.")
@RestController
@RequestMapping("/api/ops/transactions")
public class OperatorTransactionsApiController {

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

    @Operation(summary = "Get a transaction detail")
    @GetMapping("/{paymentId}")
    public PaymentDetailView detail(@PathVariable UUID paymentId) {
        return paymentQueryApplicationService.getDetail(paymentId);
    }
}
