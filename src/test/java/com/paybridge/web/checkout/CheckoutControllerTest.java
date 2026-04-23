package com.paybridge.web.checkout;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CheckoutController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void rendersCheckoutPage() throws Exception {
        PayBridgeProperties.App app = new PayBridgeProperties.App();
        PayBridgeProperties.FeatureFlags flags = new PayBridgeProperties.FeatureFlags();
        flags.setStripeEnabled(true);
        flags.setNicepayEnabled(true);
        PayBridgeProperties.ProviderProperties providers = new PayBridgeProperties.ProviderProperties();
        providers.getStripe().setEnabled(true);
        providers.getStripe().setPublishableKey("pk_test_123");
        providers.getStripe().setSecretKey("sk_test_123");
        providers.getNicepay().setEnabled(true);
        providers.getNicepay().setMerchantId("test-mid-configured");
        providers.getNicepay().setMerchantKey("test-merchant-key-configured");
        given(payBridgeProperties.getApp()).willReturn(app);
        given(payBridgeProperties.getFeatures()).willReturn(flags);
        given(payBridgeProperties.getProviders()).willReturn(providers);

        mockMvc.perform(get("/checkout"))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("Choose a test payment path")))
            .andExpect(content().string(Matchers.containsString("NicePay payment test")))
            .andExpect(content().string(Matchers.containsString("Public real card test")))
            .andExpect(content().string(Matchers.containsString("real temporary charge")))
            .andExpect(content().string(Matchers.containsString("Open NicePay payment test")));
    }

    @Test
    void rendersProviderSpecificUnavailableMessages() throws Exception {
        PayBridgeProperties.App app = new PayBridgeProperties.App();
        PayBridgeProperties.FeatureFlags flags = new PayBridgeProperties.FeatureFlags();
        flags.setStripeEnabled(true);
        flags.setNicepayEnabled(true);
        PayBridgeProperties.ProviderProperties providers = new PayBridgeProperties.ProviderProperties();
        providers.getStripe().setEnabled(true);
        providers.getNicepay().setEnabled(true);
        given(payBridgeProperties.getApp()).willReturn(app);
        given(payBridgeProperties.getFeatures()).willReturn(flags);
        given(payBridgeProperties.getProviders()).willReturn(providers);

        mockMvc.perform(get("/checkout"))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("Stripe test keys are not configured in this environment.")))
            .andExpect(content().string(Matchers.containsString("NicePay test MID and merchant key are not configured in this environment.")));
    }
}
