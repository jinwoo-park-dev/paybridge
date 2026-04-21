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
        model.addAttribute("pageTitle", "Choose payment flow");
        model.addAttribute("operatorLoginUrl", "/operator/login");
        model.addAttribute("demoOrderNote", "Each provider link starts a fresh demo order ID so repeated test payments do not reuse the same PaymentIntent or provider transaction.");
        model.addAttribute("selectionNotes", List.of(
                "Start here to choose the provider-specific execution path.",
                "Stripe is the primary browser checkout route and records a server-created PaymentIntent into the shared payment model.",
                "NicePay key-in is available as a public test route when feature flags and provider test credentials are enabled."
        ));
        model.addAttribute("checkoutOptions", List.of(
                stripeOption(),
                nicePayOption()
        ));
        return "checkout/index";
    }

    private CheckoutOptionView stripeOption() {
        boolean enabled = payBridgeProperties.getFeatures().isStripeEnabled()
                && payBridgeProperties.getProviders().getStripe().isEnabled();

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
                "Stripe Payment Element",
                "Primary browser checkout",
                "Server-created PaymentIntent with Stripe-hosted confirmation and shared payment recording.",
                actionUrl,
                enabled,
                false,
                List.of(
                        "Backend creates the PaymentIntent before browser confirmation",
                        "Return-page verification, webhooks, and refunds converge on the same payment record",
                        "Browser card entry stays inside Stripe-hosted fields"
                )
        );
    }

    private CheckoutOptionView nicePayOption() {
        boolean enabled = payBridgeProperties.getFeatures().isNicepayEnabled()
                && payBridgeProperties.getProviders().getNicepay().isEnabled();

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
                "NicePay key-in",
                "Public test flow",
                "Merchant-hosted key-in flow retained for provider-specific approval and cancellation handling.",
                actionUrl,
                enabled,
                false,
                List.of(
                        "Approval, full cancellation, and partial cancellation stay on the provider path",
                        "Provider-specific identifiers are stored on the shared payment record",
                        "Merchant-hosted card entry should use only provider test credentials and test card data"
                )
        );
    }
}
