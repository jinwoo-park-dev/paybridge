package com.paybridge.support.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paybridge")
public class PayBridgeProperties {

    private final App app = new App();
    private final FeatureFlags features = new FeatureFlags();
    private final ProviderProperties providers = new ProviderProperties();
    private final Security security = new Security();

    public App getApp() {
        return app;
    }

    public FeatureFlags getFeatures() {
        return features;
    }

    public ProviderProperties getProviders() {
        return providers;
    }

    public Security getSecurity() {
        return security;
    }

    public static class App {
        private String displayName = "PayBridge";
        private String subtitle = "Payment Orchestration Service";
        private String uiStrategy = "Spring MVC + server-rendered pages + minimal JS";
        private String architectureStyle = "Modular Monolith";

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getUiStrategy() {
            return uiStrategy;
        }

        public void setUiStrategy(String uiStrategy) {
            this.uiStrategy = uiStrategy;
        }

        public String getArchitectureStyle() {
            return architectureStyle;
        }

        public void setArchitectureStyle(String architectureStyle) {
            this.architectureStyle = architectureStyle;
        }
    }

    public static class FeatureFlags {
        private boolean nicepayEnabled;
        private boolean stripeEnabled;
        private boolean nicepayLocalOnly;
        private boolean unifiedCheckoutEnabled;
        private boolean operatorApiEnabled;

        public boolean isNicepayEnabled() { return nicepayEnabled; }
        public void setNicepayEnabled(boolean nicepayEnabled) { this.nicepayEnabled = nicepayEnabled; }
        public boolean isStripeEnabled() { return stripeEnabled; }
        public void setStripeEnabled(boolean stripeEnabled) { this.stripeEnabled = stripeEnabled; }
        public boolean isNicepayLocalOnly() { return nicepayLocalOnly; }
        public void setNicepayLocalOnly(boolean nicepayLocalOnly) { this.nicepayLocalOnly = nicepayLocalOnly; }
        public boolean isUnifiedCheckoutEnabled() { return unifiedCheckoutEnabled; }
        public void setUnifiedCheckoutEnabled(boolean unifiedCheckoutEnabled) { this.unifiedCheckoutEnabled = unifiedCheckoutEnabled; }
        public boolean isOperatorApiEnabled() { return operatorApiEnabled; }
        public void setOperatorApiEnabled(boolean operatorApiEnabled) { this.operatorApiEnabled = operatorApiEnabled; }
    }

    public static class Security {
        private String operatorUsername = "operator";
        private String operatorPassword = "operator-change-me";
        private List<String> consoleAllowedOrigins = new ArrayList<>(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));

        public String getOperatorUsername() { return operatorUsername; }
        public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }
        public String getOperatorPassword() { return operatorPassword; }
        public void setOperatorPassword(String operatorPassword) { this.operatorPassword = operatorPassword; }
        public List<String> getConsoleAllowedOrigins() { return consoleAllowedOrigins; }
        public void setConsoleAllowedOrigins(List<String> consoleAllowedOrigins) { this.consoleAllowedOrigins = consoleAllowedOrigins; }
    }

    public static class ProviderProperties {
        private final NicePay nicepay = new NicePay();
        private final Stripe stripe = new Stripe();

        public NicePay getNicepay() { return nicepay; }
        public Stripe getStripe() { return stripe; }
    }

    public static class NicePay {
        private boolean enabled;
        private String baseUrl = "https://webapi.nicepay.co.kr/webapi";
        private String merchantId;
        private String merchantKey;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        public String getMerchantKey() { return merchantKey; }
        public void setMerchantKey(String merchantKey) { this.merchantKey = merchantKey; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    }

    public static class Stripe {
        private boolean enabled;
        private String baseUrl = "https://api.stripe.com";
        private String publishableKey;
        private String secretKey;
        private String webhookSigningSecret;
        private String defaultCurrency = "USD";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getPublishableKey() { return publishableKey; }
        public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getWebhookSigningSecret() { return webhookSigningSecret; }
        public void setWebhookSigningSecret(String webhookSigningSecret) { this.webhookSigningSecret = webhookSigningSecret; }
        public String getDefaultCurrency() { return defaultCurrency; }
        public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    }
}
