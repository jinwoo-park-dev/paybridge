package com.paybridge.support.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paybridge.support.test.AbstractPostgresIntegrationTest;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperatorSecurityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedPagesRedirectAnonymousUsersToOperatorLogin() throws Exception {
        mockMvc.perform(get("/ops/transactions/search"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/operator/login"));
    }

    @Test
    void operatorCanAccessProtectedPage() throws Exception {
        mockMvc.perform(get("/ops/transactions/search").with(user("operator").roles("OPERATOR")))
            .andExpect(status().isOk());
    }

    @Test
    void stripeCheckoutRemainsPublic() throws Exception {
        mockMvc.perform(get("/payments/stripe/checkout"))
            .andExpect(status().isOk());
    }


    @Test
    void nicePayKeyInRemainsPublicForAnonymousDemoUsers() throws Exception {
        mockMvc.perform(get("/payments/nicepay/keyin"))
            .andExpect(status().isOk());
    }

    @Test
    void nicePayResultRemainsPublicForAnonymousDemoUsers() throws Exception {
        mockMvc.perform(get("/payments/nicepay/result").param("paymentId", "11111111-1111-1111-1111-111111111111"))
            .andExpect(status().isNotFound());
    }

    @Test
    void operatorApiRejectsAnonymousUsersWhenEnabled() throws Exception {
        mockMvc.perform(get("/api/ops/transactions"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void operatorApiAllowsAuthenticatedOperatorsWhenEnabled() throws Exception {
        mockMvc.perform(get("/api/ops/transactions").with(user("operator").roles("OPERATOR")))
            .andExpect(status().isOk());
    }

    @Test
    void authenticatedOperatorCanLogoutFromUiFlow() throws Exception {
        mockMvc.perform(post("/operator/logout")
                .accept(MediaType.TEXT_HTML)
                .with(csrf())
                .with(user("operator").roles("OPERATOR")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/operator/login?logout"));
    }

}
