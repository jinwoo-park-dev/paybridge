package com.paybridge.web.home;

import com.paybridge.support.config.PayBridgeProperties;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class HomeController {

    private final Environment environment;
    private final PayBridgeProperties payBridgeProperties;

    public HomeController(Environment environment, PayBridgeProperties payBridgeProperties) {
        this.environment = environment;
        this.payBridgeProperties = payBridgeProperties;
    }

    @GetMapping("/")
    public String home(Model model) {
        boolean stripeEnabled = payBridgeProperties.getFeatures().isStripeEnabled()
                && payBridgeProperties.getProviders().getStripe().isEnabled();
        boolean nicepayEnabled = payBridgeProperties.getFeatures().isNicepayEnabled()
                && payBridgeProperties.getProviders().getNicepay().isEnabled();

        model.addAttribute("projectName", payBridgeProperties.getApp().getDisplayName());
        model.addAttribute("projectSubtitle", payBridgeProperties.getApp().getSubtitle());
        model.addAttribute("uiStrategy", payBridgeProperties.getApp().getUiStrategy());
        model.addAttribute("architectureStyle", payBridgeProperties.getApp().getArchitectureStyle());
        model.addAttribute("activeProfiles", String.join(", ", environment.getActiveProfiles()));
        model.addAttribute("stripeEnabled", stripeEnabled);
        model.addAttribute("nicepayEnabled", nicepayEnabled);
        model.addAttribute("unifiedCheckoutEnabled", payBridgeProperties.getFeatures().isUnifiedCheckoutEnabled());
        model.addAttribute("operatorApiEnabled", payBridgeProperties.getFeatures().isOperatorApiEnabled());
        model.addAttribute("productBullets", List.of(
                "Shared payment lifecycle across Stripe PaymentIntents and NicePay key-in approvals.",
                "One transaction record for approval, refund, full cancellation, and partial reversal flows.",
                "Operator search and payment detail pages backed by read-only JSON endpoints for transactions, audit logs, outbox events, and recent Stripe webhooks.",
                "Idempotency, duplicate-safe webhook handling, correlation IDs, masked logging, and database-backed audit/outbox persistence."
        ));
        model.addAttribute("milestones", List.of(
                "Checkout entry that routes into Stripe or NicePay without flattening provider-specific behavior.",
                "Stripe browser checkout with return-page verification and webhook follow-up.",
                "NicePay public test key-in flow with operator-only full cancellation and partial cancellation.",
                "Transaction search, payment detail, and related JSON endpoints for operational inspection."
        ));
        return "home/index";
    }
}
