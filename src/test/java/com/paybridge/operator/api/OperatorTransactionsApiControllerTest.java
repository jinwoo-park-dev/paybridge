package com.paybridge.operator.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.application.PaymentReversalView;
import com.paybridge.payment.application.TransactionExportPageView;
import com.paybridge.payment.application.TransactionExportView;
import com.paybridge.payment.application.TransactionSummaryView;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import com.paybridge.payment.domain.ReversalStatus;
import com.paybridge.payment.domain.ReversalType;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import java.time.Instant;
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
    void returnsMachineReadableTransactionExport() throws Exception {
        TransactionExportPageView exportPage = new TransactionExportPageView(
                List.of(
                        new TransactionExportView(
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "ORD-2001",
                                PaymentProvider.STRIPE,
                                PaymentStatus.APPROVED,
                                1_999L,
                                1_999L,
                                "USD",
                                "pi_123",
                                "ch_123",
                                Instant.parse("2026-03-21T12:00:00Z"),
                                Instant.parse("2026-03-21T12:00:01Z"),
                                Instant.parse("2026-03-21T12:00:02Z")
                        )
                ),
                0,
                200,
                false
        );

        given(paymentQueryApplicationService.export(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(200)))
                .willReturn(exportPage);

        mockMvc.perform(get("/api/ops/transactions/export")
                        .param("approvedFrom", "2026-03-21T00:00:00Z")
                        .param("approvedTo", "2026-03-22T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"amountMinor\":1999")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"approvedAt\":\"2026-03-21T12:00:00Z\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"page\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"size\":200")));
    }

    @Test
    void rejectsOversizedTransactionExportPageRequest() throws Exception {
        mockMvc.perform(get("/api/ops/transactions/export").param("size", "501"))
                .andExpect(status().isBadRequest());
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"reversals\"")))
                .andExpect(jsonPath("$.reversals[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.reversals[0].reversalStatus").doesNotExist());
    }
}
