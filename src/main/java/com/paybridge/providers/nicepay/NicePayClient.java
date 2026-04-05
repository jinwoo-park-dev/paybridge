package com.paybridge.providers.nicepay;

import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class NicePayClient {

    private static final String APPROVAL_PATH = "/card_keyin.jsp";
    private static final String CANCEL_PATH = "/cancel_process.jsp";

    private final PayBridgeProperties payBridgeProperties;
    private final EucKrFormClient eucKrFormClient;
    private final NicePayCryptoSupport nicePayCryptoSupport;

    public NicePayClient(
            PayBridgeProperties payBridgeProperties,
            EucKrFormClient eucKrFormClient,
            NicePayCryptoSupport nicePayCryptoSupport
    ) {
        this.payBridgeProperties = payBridgeProperties;
        this.eucKrFormClient = eucKrFormClient;
        this.nicePayCryptoSupport = nicePayCryptoSupport;
    }

    public NicePayApprovalResponse approve(NicePayApprovalRequest request) {
        PayBridgeProperties.NicePay properties = requireConfiguredProvider();
        Map<String, String> rawResponse = eucKrFormClient.postForm(
                endpoint(properties.getBaseUrl(), APPROVAL_PATH),
                request.toFormParameters(),
                properties.getConnectTimeout(),
                properties.getReadTimeout()
        );
        NicePayApprovalResponse response = NicePayApprovalResponse.from(rawResponse);
        if (!response.isSuccess()) {
            throw new PayBridgeException(
                    HttpStatus.BAD_GATEWAY,
                    ErrorCode.PROVIDER_ERROR,
                    "NicePay approval failed [" + response.resultCode() + "] " + response.resultMessage()
            );
        }
        return response;
    }

    public NicePayCancelResponse cancel(NicePayCancelRequest request) {
        PayBridgeProperties.NicePay properties = requireConfiguredProvider();
        Map<String, String> rawResponse = eucKrFormClient.postForm(
                endpoint(properties.getBaseUrl(), CANCEL_PATH),
                request.toFormParameters(),
                properties.getConnectTimeout(),
                properties.getReadTimeout()
        );
        NicePayCancelResponse response = NicePayCancelResponse.from(rawResponse);
        if (!response.isSuccess()) {
            throw new PayBridgeException(
                    HttpStatus.BAD_GATEWAY,
                    ErrorCode.PROVIDER_ERROR,
                    "NicePay cancellation failed [" + response.resultCode() + "] " + response.resultMessage()
                            + (response.errorCode().isBlank() ? "" : " / " + response.errorCode() + " " + response.errorMessage())
            );
        }

        String expectedSignature = nicePayCryptoSupport.expectedCancelResponseSignature(
                response.tid(),
                request.merchantId(),
                response.cancelAmount(),
                properties.getMerchantKey()
        );
        if (!expectedSignature.equalsIgnoreCase(response.signature())) {
            throw new PayBridgeException(
                    HttpStatus.BAD_GATEWAY,
                    ErrorCode.PROVIDER_ERROR,
                    "NicePay cancellation response signature verification failed."
            );
        }
        return response;
    }

    private PayBridgeProperties.NicePay requireConfiguredProvider() {
        boolean featureEnabled = payBridgeProperties.getFeatures().isNicepayEnabled();
        PayBridgeProperties.NicePay provider = payBridgeProperties.getProviders().getNicepay();
        if (!featureEnabled || !provider.isEnabled()) {
            throw new PayBridgeException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.PROVIDER_ERROR,
                    "NicePay integration is disabled. Enable paybridge.features.nicepay-enabled and paybridge.providers.nicepay.enabled first."
            );
        }
        if (provider.getMerchantId() == null || provider.getMerchantId().isBlank() || provider.getMerchantKey() == null || provider.getMerchantKey().isBlank()) {
            throw new PayBridgeException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.PROVIDER_ERROR,
                    "NicePay merchant configuration is incomplete. Provide PAYBRIDGE_NICEPAY_MID and PAYBRIDGE_NICEPAY_MERCHANT_KEY."
            );
        }
        return provider;
    }

    private URI endpoint(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalizedBase + path);
    }
}
