package com.paybridge.web.system;

import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemInfoController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayBridgeProperties payBridgeProperties;

    @MockBean
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void returnsSystemInfo() throws Exception {
        PayBridgeProperties.FeatureFlags flags = new PayBridgeProperties.FeatureFlags();
        flags.setUnifiedCheckoutEnabled(true);
        flags.setOperatorApiEnabled(true);
        given(payBridgeProperties.getApp()).willReturn(new PayBridgeProperties.App());
        given(payBridgeProperties.getFeatures()).willReturn(flags);

        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("paybridge"))
                .andExpect(jsonPath("$.releaseVersion").value("0.1.0"))
                .andExpect(jsonPath("$.unifiedCheckoutEnabled").value(true))
                .andExpect(jsonPath("$.operatorApiEnabled").value(true));
    }
}
