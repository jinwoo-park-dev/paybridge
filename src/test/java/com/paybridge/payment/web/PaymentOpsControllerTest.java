package com.paybridge.payment.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.application.PaymentReversalView;
import com.paybridge.payment.application.TransactionSummaryView;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import com.paybridge.payment.domain.ReversalStatus;
import com.paybridge.payment.domain.ReversalType;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentOpsController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentOpsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentQueryApplicationService paymentQueryApplicationService;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void rendersTransactionSearchPage() throws Exception {
        TransactionSummaryView summaryView = new TransactionSummaryView(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ORD-1001",
                PaymentProvider.NICEPAY,
                PaymentStatus.APPROVED,
                "KRW 10,000",
                "KRW 10,000",
                "-",
                "TID-1001",
                "2026-03-19 12:00:00 UTC"
        );
        given(paymentQueryApplicationService.search(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(summaryView));

        mockMvc.perform(get("/ops/transactions/search"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Transaction Search")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ORD-1001")));
    }

    @Test
    void rendersPaymentDetailPage() throws Exception {
        UUID paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PaymentDetailView detailView = new PaymentDetailView(
                paymentId,
                "ORD-1001",
                PaymentProvider.STRIPE,
                PaymentStatus.PARTIALLY_REVERSED,
                "USD 120.00",
                "USD 20.00",
                "USD",
                "pi_123",
                "-",
                "2026-03-19 12:00:00 UTC",
                true,
                true,
                List.of(new PaymentReversalView(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        ReversalType.PARTIAL,
                        ReversalStatus.SUCCEEDED,
                        "USD 100.00",
                        "USD 20.00",
                        "customer requested partial refund",
                        "re_123",
                        "2026-03-19 12:10:00 UTC"
                ))
        );
        given(paymentQueryApplicationService.getDetail(paymentId)).willReturn(detailView);

        mockMvc.perform(get("/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment Detail")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ORD-1001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stripe refund actions")));
    }
}
