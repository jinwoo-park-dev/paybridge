package com.paybridge.payment.web;

import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.application.TransactionSearchCriteria;
import com.paybridge.payment.application.TransactionSummaryView;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Hidden
@Controller
@RequestMapping
public class PaymentOpsController {

    private final PaymentQueryApplicationService paymentQueryApplicationService;

    public PaymentOpsController(PaymentQueryApplicationService paymentQueryApplicationService) {
        this.paymentQueryApplicationService = paymentQueryApplicationService;
    }

    @GetMapping("/ops/transactions/search")
    public String search(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String providerPaymentId,
            @RequestParam(required = false) String providerTransactionId,
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) PaymentStatus status,
            Model model
    ) {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                orderId,
                providerPaymentId,
                providerTransactionId,
                provider,
                status
        );
        List<TransactionSummaryView> results = paymentQueryApplicationService.search(criteria);

        model.addAttribute("criteria", criteria);
        model.addAttribute("results", results);
        model.addAttribute("providers", PaymentProvider.values());
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("resultCount", results.size());
        return "payment/ops-search";
    }

    @GetMapping("/payments/{paymentId}")
    public String detail(@PathVariable UUID paymentId, Model model) {
        PaymentDetailView payment = paymentQueryApplicationService.getDetail(paymentId);
        model.addAttribute("payment", payment);
        model.addAttribute("reversalCount", payment.reversals().size());
        return "payment/detail";
    }
}
