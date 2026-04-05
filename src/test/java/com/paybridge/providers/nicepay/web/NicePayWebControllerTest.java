package com.paybridge.providers.nicepay.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.providers.nicepay.NicePayApprovalApplicationService;
import com.paybridge.providers.nicepay.NicePayApprovalOutcome;
import com.paybridge.providers.nicepay.NicePayCancellationApplicationService;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
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
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void rendersKeyInPage() throws Exception {
        stubConfiguration();

        mockMvc.perform(get("/payments/nicepay/keyin"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("NicePay key-in")));
    }

    @Test
    void approvalSubmissionRedirectsToPaymentDetail() throws Exception {
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
                        .param("cardQuota", "00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/11111111-1111-1111-1111-111111111111"));
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
                        .param("cardQuota", "00"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("4111111111111111"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("2512"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("800101"))));
    }

    private void stubConfiguration() {
        PayBridgeProperties.FeatureFlags featureFlags = new PayBridgeProperties.FeatureFlags();
        PayBridgeProperties.ProviderProperties providerProperties = new PayBridgeProperties.ProviderProperties();
        providerProperties.getNicepay().setMerchantId("nictest04m");
        given(payBridgeProperties.getFeatures()).willReturn(featureFlags);
        given(payBridgeProperties.getProviders()).willReturn(providerProperties);
    }
}
