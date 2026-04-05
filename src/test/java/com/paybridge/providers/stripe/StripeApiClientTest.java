package com.paybridge.providers.stripe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.support.config.PayBridgeProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class StripeApiClientTest {

    private HttpServer server;
    private volatile String lastAuthorization;
    private volatile String lastRequestBody;
    private StripeApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        PayBridgeProperties properties = new PayBridgeProperties();
        properties.getFeatures().setStripeEnabled(true);
        properties.getProviders().getStripe().setEnabled(true);
        properties.getProviders().getStripe().setPublishableKey("pk_test_123");
        properties.getProviders().getStripe().setSecretKey("sk_test_123");
        properties.getProviders().getStripe().setBaseUrl("http://localhost:" + server.getAddress().getPort());

        client = new StripeApiClient(properties, new ObjectMapper(), RestClient.builder());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createsPaymentIntentViaFormPost() {
        server.createContext("/v1/payment_intents", exchange -> {
            captureRequest(exchange);
            byte[] body = ("{"
                    + "\"id\":\"pi_paybridge_123\"," 
                    + "\"client_secret\":\"pi_paybridge_secret_123\"," 
                    + "\"status\":\"requires_payment_method\"," 
                    + "\"amount\":1999," 
                    + "\"currency\":\"usd\"," 
                    + "\"latest_charge\":null," 
                    + "\"metadata\":{\"order_id\":\"ORD-STR-2026-1001\"}}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        StripePaymentIntentResponse response = client.createPaymentIntent(
                new StripeCreatePaymentIntentRequest(
                        "ORD-STR-2026-1001",
                        1999L,
                        "USD",
                        "Monthly plan renewal",
                        "buyer@example.com",
                        "paybridge-stripe-intent-ORD-STR-2026-1001"
                )
        );

        assertThat(lastAuthorization).startsWith("Basic ");
        assertThat(lastRequestBody).contains("amount=1999");
        assertThat(lastRequestBody).contains("metadata%5Border_id%5D=ORD-STR-2026-1001");
        assertThat(lastRequestBody).contains("automatic_payment_methods%5Benabled%5D=true");
        assertThat(response.id()).isEqualTo("pi_paybridge_123");
        assertThat(response.clientSecret()).isEqualTo("pi_paybridge_secret_123");
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void retrievesPaymentIntentAndParsesLatestCharge() {
        server.createContext("/v1/payment_intents/pi_paybridge_123", exchange -> {
            captureRequest(exchange);
            byte[] body = ("{"
                    + "\"id\":\"pi_paybridge_123\"," 
                    + "\"status\":\"succeeded\"," 
                    + "\"amount\":1999," 
                    + "\"currency\":\"usd\"," 
                    + "\"latest_charge\":\"ch_paybridge_123\"," 
                    + "\"metadata\":{\"order_id\":\"ORD-STR-2026-1001\"}}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        StripePaymentIntentResponse response = client.retrievePaymentIntent("pi_paybridge_123");

        assertThat(response.status()).isEqualTo("succeeded");
        assertThat(response.latestChargeId()).isEqualTo("ch_paybridge_123");
        assertThat(response.orderId()).isEqualTo("ORD-STR-2026-1001");
    }

    @Test
    void createsPartialRefundUsingChargeReference() {
        server.createContext("/v1/refunds", exchange -> {
            captureRequest(exchange);
            byte[] body = ("{"
                    + "\"id\":\"re_paybridge_123\"," 
                    + "\"status\":\"succeeded\"," 
                    + "\"amount\":500," 
                    + "\"currency\":\"usd\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        StripeRefundResponse response = client.createRefund(
                new StripeRefundRequest(
                        "pi_paybridge_123",
                        "ch_paybridge_123",
                        500L,
                        "Customer requested partial refund.",
                        "stripe-refund:payment:partial:500"
                )
        );

        assertThat(lastRequestBody).contains("charge=ch_paybridge_123");
        assertThat(lastRequestBody).contains("amount=500");
        assertThat(response.id()).isEqualTo("re_paybridge_123");
        assertThat(response.isSucceeded()).isTrue();
    }

    private void captureRequest(HttpExchange exchange) throws IOException {
        lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
