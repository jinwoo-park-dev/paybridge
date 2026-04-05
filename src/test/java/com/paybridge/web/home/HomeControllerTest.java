package com.paybridge.web.home;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void rendersHomePage() throws Exception {
        PayBridgeProperties.App app = new PayBridgeProperties.App();
        PayBridgeProperties.FeatureFlags features = new PayBridgeProperties.FeatureFlags();
        PayBridgeProperties.ProviderProperties providers = new PayBridgeProperties.ProviderProperties();
        features.setUnifiedCheckoutEnabled(true);
        features.setOperatorApiEnabled(true);
        features.setStripeEnabled(true);
        features.setNicepayEnabled(false);
        providers.getStripe().setEnabled(true);
        providers.getNicepay().setEnabled(false);
        given(payBridgeProperties.getApp()).willReturn(app);
        given(payBridgeProperties.getFeatures()).willReturn(features);
        given(payBridgeProperties.getProviders()).willReturn(providers);

        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("PayBridge")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Open checkout")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Enabled")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Requires local configuration")));
    }

    @Test
    void rendersLogoutActionForAuthenticatedOperator() throws Exception {
        PayBridgeProperties.App app = new PayBridgeProperties.App();
        PayBridgeProperties.FeatureFlags features = new PayBridgeProperties.FeatureFlags();
        PayBridgeProperties.ProviderProperties providers = new PayBridgeProperties.ProviderProperties();
        features.setUnifiedCheckoutEnabled(true);
        features.setOperatorApiEnabled(true);
        features.setStripeEnabled(true);
        features.setNicepayEnabled(false);
        providers.getStripe().setEnabled(true);
        providers.getNicepay().setEnabled(false);
        given(payBridgeProperties.getApp()).willReturn(app);
        given(payBridgeProperties.getFeatures()).willReturn(features);
        given(payBridgeProperties.getProviders()).willReturn(providers);

        UsernamePasswordAuthenticationToken operatorAuthentication = new UsernamePasswordAuthenticationToken(
            "operator",
            "n/a",
            AuthorityUtils.createAuthorityList("ROLE_OPERATOR")
        );

        mockMvc.perform(get("/").principal(operatorAuthentication))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Operator sign-out")));
    }

}
