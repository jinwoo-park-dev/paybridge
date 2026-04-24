package com.paybridge.web.system;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorResponseFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void returnsMinimalPublicSystemInfo() throws Exception {
        given(payBridgeProperties.getApp()).willReturn(new PayBridgeProperties.App());

        mockMvc.perform(get("/api/system/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("paybridge"))
            .andExpect(jsonPath("$.project").value("PayBridge"))
            .andExpect(jsonPath("$.releaseVersion").value("1.0.0"))
            .andExpect(jsonPath("$.demoType").value("Public portfolio demo for test payment flows"))
            .andExpect(jsonPath("$.publicSurfaces", hasItem("Checkout selection")))
            .andExpect(jsonPath("$.protectedSurfaces", hasItem("Payment detail")))
            .andExpect(jsonPath("$.frontendChoice").doesNotExist())
            .andExpect(jsonPath("$.architectureStyle").doesNotExist())
            .andExpect(jsonPath("$.activeProfiles").doesNotExist())
            .andExpect(jsonPath("$.unifiedCheckoutEnabled").doesNotExist())
            .andExpect(jsonPath("$.protectedSurfaces", not(hasItem("Modular Monolith"))));
    }
}
