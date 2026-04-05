package com.paybridge.operator.api;

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

@WebMvcTest(OperatorTransactionsApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperatorTransactionsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentQueryApplicationService paymentQueryApplicationService;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void returnsTransactionSearchResults() throws Exception {
        TransactionSummaryView summary = new TransactionSummaryView(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ORD-2001",
                PaymentProvider.STRIPE,
                PaymentStatus.APPROVED,
                "USD 19.99",
                "USD 19.99",
                "pi_123",
                "-",
                "2026-03-21 12:00:00 UTC"
        );

        given(paymentQueryApplicationService.search(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(summary));

        mockMvc.perform(get("/api/ops/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"orderId\":\"ORD-2001\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"provider\":\"STRIPE\"")));
    }

    @Test
    void returnsTransactionDetail() throws Exception {
        UUID paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        PaymentDetailView detail = new PaymentDetailView(
                paymentId,
                "ORD-2001",
                PaymentProvider.NICEPAY,
                PaymentStatus.PARTIALLY_REVERSED,
                "KRW 10,004",
                "KRW 5,004",
                "KRW",
                "cp_123",
                "TID-1001",
                "2026-03-21 12:00:00 UTC",
                true,
                true,
                List.of(
                        new PaymentReversalView(
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                ReversalType.PARTIAL,
                                ReversalStatus.SUCCEEDED,
                                "KRW 5,000",
                                "KRW 5,004",
                                "partial cancel",
                                "cancel_123",
                                "2026-03-21 12:10:00 UTC"
                        )
                )
        );

        given(paymentQueryApplicationService.getDetail(paymentId)).willReturn(detail);

        mockMvc.perform(get("/api/ops/transactions/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"orderId\":\"ORD-2001\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"provider\":\"NICEPAY\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"reversals\"")));
    }
}
