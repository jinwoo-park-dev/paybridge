package com.paybridge.web.checkout;

import com.paybridge.providers.nicepay.web.NicePayKeyInForm;
import com.paybridge.providers.stripe.web.StripeCheckoutForm;
import com.paybridge.support.config.PayBridgeProperties;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

@Hidden
@Controller
public class CheckoutController {

    private final PayBridgeProperties payBridgeProperties;

    public CheckoutController(PayBridgeProperties payBridgeProperties) {
        this.payBridgeProperties = payBridgeProperties;
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("projectName", payBridgeProperties.getApp().getDisplayName());
        model.addAttribute("pageTitle", "Choose a test payment path");
        model.addAttribute("operatorLoginUrl", "/operator/login");
        model.addAttribute("demoOrderNote", "Each provider link starts with a fresh demo order ID so repeated tests do not reuse the same recorded payment flow.");
        model.addAttribute("selectionNotes", List.of(
            "Choose the provider path from one entry page while PayBridge records the result in one shared payment lifecycle.",
            "Stripe uses a browser checkout backed by a server created PaymentIntent.",
            "NicePay uses test merchant credentials, but it can still create a real temporary card charge that is automatically canceled before midnight on the same day.",
            "Operator login is still required for transaction detail, refunds, cancellations, audit records, and exports."
        ));
        model.addAttribute("checkoutOptions", List.of(
            stripeOption(),
            nicePayOption()
        ));
        return "checkout/index";
    }

    private CheckoutOptionView stripeOption() {
        PayBridgeProperties.FeatureFlags features = payBridgeProperties.getFeatures();
        PayBridgeProperties.Stripe stripe = payBridgeProperties.getProviders().getStripe();
        boolean featureEnabled = features.isStripeEnabled() && stripe.isEnabled();
        boolean credentialsConfigured = hasText(stripe.getPublishableKey()) && hasText(stripe.getSecretKey());
        Availability availability = availability(
            featureEnabled,
            credentialsConfigured,
            "Stripe test keys are not configured in this environment."
        );

        String actionUrl = UriComponentsBuilder.fromPath("/payments/stripe/checkout")
            .queryParam("orderId", StripeCheckoutForm.newDemoOrderId())
            .queryParam("amountMinor", 1999)
            .queryParam("currency", "USD")
            .queryParam("description", "Monthly plan renewal")
            .queryParam("customerEmail", "buyer@example.com")
            .build()
            .toUriString();

        return new CheckoutOptionView(
            "stripe",
            "Stripe browser checkout",
            "Primary test card flow",
            "PayBridge creates the PaymentIntent on the server, then Stripe collects card details in the browser.",
            actionUrl,
            "Open Stripe checkout",
            availability.enabled(),
            false,
            availability.unavailableMessage(),
            List.of(
                "Server creates the PaymentIntent before browser confirmation",
                "Stripe fields collect card details without exposing them to PayBridge",
                "The return page and webhook path converge on the same recorded payment"
            )
        );
    }

    private CheckoutOptionView nicePayOption() {
        PayBridgeProperties.FeatureFlags features = payBridgeProperties.getFeatures();
        PayBridgeProperties.NicePay nicePay = payBridgeProperties.getProviders().getNicepay();
        boolean featureEnabled = features.isNicepayEnabled() && nicePay.isEnabled();
        boolean credentialsConfigured = hasText(nicePay.getMerchantId()) && hasText(nicePay.getMerchantKey());
        Availability availability = availability(
            featureEnabled,
            credentialsConfigured,
            "NicePay test MID and merchant key are not configured in this environment."
        );

        String actionUrl = UriComponentsBuilder.fromPath("/payments/nicepay/keyin")
            .queryParam("orderId", NicePayKeyInForm.newDefaultOrderId())
            .queryParam("amountMinor", 10000)
            .queryParam("goodsName", "Monthly plan renewal")
            .queryParam("buyerName", "Alex Kim")
            .queryParam("buyerEmail", "buyer@example.com")
            .queryParam("buyerTel", "01012345678")
            .build()
            .toUriString();

        return new CheckoutOptionView(
            "nicepay",
            "NicePay payment test",
            "Public real card test",
            "This route uses NicePay test merchant credentials, but the submitted card can still receive a real temporary charge before the provider cancels it later the same day.",
            actionUrl,
            "Open NicePay payment test",
            availability.enabled(),
            false,
            availability.unavailableMessage(),
            List.of(
                "The tester enters a real card and personal information they are authorized to use",
                "NicePay automatically cancels the temporary test charge before midnight on the same day",
                "PayBridge records the approval result while internal detail, refund, and cancellation actions stay behind operator access"
            )
        );
    }

    private Availability availability(boolean featureEnabled, boolean credentialsConfigured, String credentialMessage) {
        if (!featureEnabled) {
            return new Availability(false, "Disabled by the active deployment settings.");
        }
        if (!credentialsConfigured) {
            return new Availability(false, credentialMessage);
        }
        return new Availability(true, "Available in this environment.");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Availability(boolean enabled, String unavailableMessage) {
    }
}
