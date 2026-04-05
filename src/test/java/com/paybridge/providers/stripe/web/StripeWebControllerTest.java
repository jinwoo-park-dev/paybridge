package com.paybridge.providers.stripe.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.providers.stripe.StripeCheckoutPageView;
import com.paybridge.providers.stripe.StripePaymentConfirmationOutcome;
import com.paybridge.providers.stripe.StripePaymentIntentApplicationService;
import com.paybridge.providers.stripe.StripeRefundApplicationService;
import com.paybridge.providers.stripe.StripeRefundOutcome;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StripeWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class StripeWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StripePaymentIntentApplicationService stripePaymentIntentApplicationService;

    @MockBean
    private StripeRefundApplicationService stripeRefundApplicationService;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void rendersStripeCheckoutPage() throws Exception {
        configureStripeProperties();

        mockMvc.perform(get("/payments/stripe/checkout"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stripe Payment Element")));
    }

    @Test
    void paymentIntentCreationRendersCheckoutSection() throws Exception {
        configureStripeProperties();
        given(stripePaymentIntentApplicationService.createCheckoutSession(any())).willReturn(
                new StripeCheckoutPageView(
                        "pi_paybridge_123",
                        "pi_secret_123",
                        "ORD-STR-2026-1001",
                        "USD 19.99",
                        "USD",
                        "Monthly plan renewal"
                )
        );

        mockMvc.perform(post("/payments/stripe/payment-intent")
                        .param("orderId", "ORD-STR-2026-1001")
                        .param("amountMinor", "1999")
                        .param("currency", "USD")
                        .param("description", "Monthly plan renewal")
                        .param("customerEmail", "buyer@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ready to confirm payment")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pi_paybridge_123")));
    }

    @Test
    void returnEndpointRedirectsToPaymentDetail() throws Exception {
        given(stripePaymentIntentApplicationService.confirmAndRecord("pi_paybridge_123")).willReturn(
                new StripePaymentConfirmationOutcome(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        false,
                        "pi_paybridge_123",
                        "succeeded"
                )
        );

        mockMvc.perform(get("/payments/stripe/return").param("payment_intent", "pi_paybridge_123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void fullRefundRedirectsBackToPaymentDetail() throws Exception {
        UUID paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        given(stripeRefundApplicationService.refundFully(paymentId, "Customer requested full refund.")).willReturn(
                new StripeRefundOutcome(paymentId, UUID.fromString("22222222-2222-2222-2222-222222222222"), false, "re_paybridge_123")
        );

        mockMvc.perform(post("/payments/stripe/{paymentId}/refund", paymentId)
                        .param("refundReason", "Customer requested full refund."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/11111111-1111-1111-1111-111111111111"));
    }

    private void configureStripeProperties() {
        PayBridgeProperties.FeatureFlags featureFlags = new PayBridgeProperties.FeatureFlags();
        featureFlags.setStripeEnabled(true);
        PayBridgeProperties.ProviderProperties providerProperties = new PayBridgeProperties.ProviderProperties();
        providerProperties.getStripe().setEnabled(true);
        providerProperties.getStripe().setPublishableKey("pk_test_1234567890");
        given(payBridgeProperties.getFeatures()).willReturn(featureFlags);
        given(payBridgeProperties.getProviders()).willReturn(providerProperties);
    }
}
