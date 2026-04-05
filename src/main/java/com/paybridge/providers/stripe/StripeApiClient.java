package com.paybridge.providers.stripe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class StripeApiClient {

    private final PayBridgeProperties payBridgeProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public StripeApiClient(
            PayBridgeProperties payBridgeProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.payBridgeProperties = payBridgeProperties;
        this.objectMapper = objectMapper;
        PayBridgeProperties.Stripe provider = payBridgeProperties.getProviders().getStripe();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(provider.getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(provider.getReadTimeout().toMillis()));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public StripePaymentIntentResponse createPaymentIntent(StripeCreatePaymentIntentRequest request) {
        requireConfiguredProvider();
        String responseBody = exchangeForm(
                HttpMethod.POST,
                "/v1/payment_intents",
                request.toFormParameters(),
                request.idempotencyKey()
        );
        return StripePaymentIntentResponse.from(readJson(responseBody));
    }

    public StripePaymentIntentResponse retrievePaymentIntent(String paymentIntentId) {
        requireConfiguredProvider();
        String responseBody = exchangeJson(HttpMethod.GET, "/v1/payment_intents/" + paymentIntentId, null);
        return StripePaymentIntentResponse.from(readJson(responseBody));
    }

    public StripeRefundResponse createRefund(StripeRefundRequest request) {
        requireConfiguredProvider();
        String responseBody = exchangeForm(
                HttpMethod.POST,
                "/v1/refunds",
                request.toFormParameters(),
                request.idempotencyKey()
        );
        return StripeRefundResponse.from(readJson(responseBody));
    }

    private String exchangeForm(HttpMethod method, String path, Map<String, String> formParameters, String idempotencyKey) {
        try {
            return restClient.method(method)
                    .uri(endpoint(path))
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(toFormBody(formParameters))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw providerError(ex);
        } catch (Exception ex) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe API call failed: " + ex.getMessage());
        }
    }

    private String exchangeJson(HttpMethod method, String path, String idempotencyKey) {
        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.method(method)
                    .uri(endpoint(path))
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                spec.header("Idempotency-Key", idempotencyKey);
            }
            return spec.retrieve().body(String.class);
        } catch (RestClientResponseException ex) {
            throw providerError(ex);
        } catch (Exception ex) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe API call failed: " + ex.getMessage());
        }
    }

    private JsonNode readJson(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception ex) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Stripe response could not be parsed as JSON.");
        }
    }

    private PayBridgeException providerError(RestClientResponseException ex) {
        String message = "Stripe API request failed";
        try {
            JsonNode errorNode = objectMapper.readTree(ex.getResponseBodyAsString()).path("error");
            String code = text(errorNode.path("code"));
            String detail = text(errorNode.path("message"));
            if (detail != null) {
                message = "Stripe API request failed" + (code == null ? "" : " [" + code + "]") + ": " + detail;
            }
        } catch (Exception ignored) {
            if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isBlank()) {
                message = message + ": " + ex.getResponseBodyAsString();
            }
        }
        return new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, message);
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String toFormBody(Map<String, String> formParameters) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : formParameters.entrySet()) {
            joiner.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return joiner.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private URI endpoint(String path) {
        String baseUrl = payBridgeProperties.getProviders().getStripe().getBaseUrl();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalizedBase + path);
    }

    private String authorizationHeader() {
        String secretKey = payBridgeProperties.getProviders().getStripe().getSecretKey();
        String credential = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + credential;
    }

    private void requireConfiguredProvider() {
        boolean featureEnabled = payBridgeProperties.getFeatures().isStripeEnabled();
        PayBridgeProperties.Stripe provider = payBridgeProperties.getProviders().getStripe();
        if (!featureEnabled || !provider.isEnabled()) {
            throw new PayBridgeException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.PROVIDER_ERROR,
                    "Stripe integration is disabled. Enable paybridge.features.stripe-enabled and paybridge.providers.stripe.enabled first."
            );
        }
        if (provider.getPublishableKey() == null || provider.getPublishableKey().isBlank()
                || provider.getSecretKey() == null || provider.getSecretKey().isBlank()) {
            throw new PayBridgeException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.PROVIDER_ERROR,
                    "Stripe configuration is incomplete. Provide PAYBRIDGE_STRIPE_PUBLISHABLE_KEY and PAYBRIDGE_STRIPE_SECRET_KEY."
            );
        }
    }
}
