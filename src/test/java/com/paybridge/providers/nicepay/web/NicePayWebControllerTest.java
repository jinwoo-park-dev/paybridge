package com.paybridge.providers.nicepay.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.payment.domain.PaymentStatus;
import com.paybridge.providers.nicepay.NicePayApprovalApplicationService;
import com.paybridge.providers.nicepay.NicePayApprovalOutcome;
import com.paybridge.providers.nicepay.NicePayCancellationApplicationService;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NicePayWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class NicePayWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NicePayApprovalApplicationService nicePayApprovalApplicationService;

    @MockBean
    private NicePayCancellationApplicationService nicePayCancellationApplicationService;

    @MockBean
    private PaymentQueryApplicationService paymentQueryApplicationService;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void rendersKeyInPage() throws Exception {
        stubConfiguration();

        mockMvc.perform(get("/payments/nicepay/keyin"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("NicePay key-in")))
                .andExpect(content().string(Matchers.containsString("Public NicePay test mode")));
    }

    @Test
    void approvalSubmissionRedirectsToPublicResultPageWithoutKeepingSensitiveFieldsInTheUrl() throws Exception {
        stubConfiguration();

        UUID paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        given(nicePayApprovalApplicationService.approve(any())).willReturn(
                new NicePayApprovalOutcome(paymentId, false, "TID-1001", "AUTH-1001")
        );

        mockMvc.perform(post("/payments/nicepay/keyin/approve")
                        .param("orderId", "ORD-NP-2026-1001")
                        .param("amountMinor", "10000")
                        .param("goodsName", "Monthly plan renewal")
                        .param("buyerName", "Alex Kim")
                        .param("buyerEmail", "buyer@example.com")
                        .param("buyerTel", "01012345678")
                        .param("cardNumber", "4111111111111111")
                        .param("cardExpireYyMm", "2512")
                        .param("buyerAuthNumber", "800101")
                        .param("cardPasswordTwoDigits", "12")
                        .param("cardInterest", "0")
                        .param("cardQuota", "00")
                        .param("testDataAcknowledged", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/nicepay/result?paymentId=11111111-1111-1111-1111-111111111111&replayed=false"));
    }

    @Test
    void rendersPublicResultPageFromRecordedPaymentWithoutEchoingCardData() throws Exception {
        stubConfiguration();

        UUID paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        given(paymentQueryApplicationService.getDetail(paymentId)).willReturn(new PaymentDetailView(
                paymentId,
                "ORD-NP-2026-1001",
                PaymentProvider.NICEPAY,
                PaymentStatus.APPROVED,
                "KRW 10,000",
                "KRW 10,000",
                "KRW",
                "TID-1001",
                "AUTH-1001",
                "2026-04-21 20:00:00 UTC",
                true,
                true,
                List.of()
        ));

        mockMvc.perform(get("/payments/nicepay/result")
                        .param("paymentId", paymentId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("NicePay approval result")))
                .andExpect(content().string(Matchers.containsString("Approval recorded successfully.")))
                .andExpect(content().string(Matchers.containsString("ORD-NP-2026-1001")))
                .andExpect(content().string(Matchers.containsString("TID-1001")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("4111111111111111"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("800101"))));
    }

    @Test
    void validationFailureDoesNotEchoSensitiveFieldsBackToThePage() throws Exception {
        stubConfiguration();

        mockMvc.perform(post("/payments/nicepay/keyin/approve")
                        .param("orderId", "ORD-NP-2026-1001")
                        .param("amountMinor", "10000")
                        .param("goodsName", "가".repeat(21))
                        .param("buyerName", "Alex Kim")
                        .param("buyerEmail", "buyer@example.com")
                        .param("buyerTel", "01012345678")
                        .param("cardNumber", "4111111111111111")
                        .param("cardExpireYyMm", "2512")
                        .param("buyerAuthNumber", "800101")
                        .param("cardPasswordTwoDigits", "12")
                        .param("cardInterest", "0")
                        .param("cardQuota", "00")
                        .param("testDataAcknowledged", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("4111111111111111"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("2512"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("800101"))));
    }

    @Test
    void requiresTestDataAcknowledgementBeforeSubmittingPublicKeyInForm() throws Exception {
        stubConfiguration();

        mockMvc.perform(post("/payments/nicepay/keyin/approve")
                        .param("orderId", "ORD-NP-2026-1001")
                        .param("amountMinor", "10000")
                        .param("goodsName", "Monthly plan renewal")
                        .param("buyerName", "Alex Kim")
                        .param("buyerEmail", "buyer@example.com")
                        .param("buyerTel", "01012345678")
                        .param("cardNumber", "4111111111111111")
                        .param("cardExpireYyMm", "2512")
                        .param("buyerAuthNumber", "800101")
                        .param("cardPasswordTwoDigits", "12")
                        .param("cardInterest", "0")
                        .param("cardQuota", "00"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Confirm that you are using provider test credentials and test card data only.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("4111111111111111"))));
    }

    private void stubConfiguration() {
        PayBridgeProperties.FeatureFlags featureFlags = new PayBridgeProperties.FeatureFlags();
        featureFlags.setNicepayEnabled(true);
        PayBridgeProperties.ProviderProperties providerProperties = new PayBridgeProperties.ProviderProperties();
        providerProperties.getNicepay().setEnabled(true);
        providerProperties.getNicepay().setMerchantId("nictest04m");
        providerProperties.getNicepay().setMerchantKey("test-merchant-key");
        given(payBridgeProperties.getFeatures()).willReturn(featureFlags);
        given(payBridgeProperties.getProviders()).willReturn(providerProperties);
    }
}
