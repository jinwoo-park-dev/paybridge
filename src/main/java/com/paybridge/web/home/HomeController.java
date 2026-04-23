package com.paybridge.web.home;

import com.paybridge.support.config.PayBridgeProperties;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class HomeController {

    private final PayBridgeProperties payBridgeProperties;

    public HomeController(PayBridgeProperties payBridgeProperties) {
        this.payBridgeProperties = payBridgeProperties;
    }

    @GetMapping("/")
    public String home(Model model) {
        PayBridgeProperties.FeatureFlags features = payBridgeProperties.getFeatures();
        PayBridgeProperties.ProviderProperties providers = payBridgeProperties.getProviders();
        PayBridgeProperties.Stripe stripe = providers.getStripe();
        PayBridgeProperties.NicePay nicepay = providers.getNicepay();

        boolean stripeAvailable = features.isStripeEnabled()
            && stripe.isEnabled()
            && hasText(stripe.getPublishableKey())
            && hasText(stripe.getSecretKey());
        boolean nicepayAvailable = features.isNicepayEnabled()
            && nicepay.isEnabled()
            && hasText(nicepay.getMerchantId())
            && hasText(nicepay.getMerchantKey());

        model.addAttribute("projectName", payBridgeProperties.getApp().getDisplayName());
        model.addAttribute("projectTagline", "Payment orchestration backend");
        model.addAttribute("stripeAvailable", stripeAvailable);
        model.addAttribute("nicepayAvailable", nicepayAvailable);
        model.addAttribute("providerUnavailableText", "Not configured in this environment");
        model.addAttribute("productBullets", List.of(
            "One shared payment and reversal lifecycle across Stripe PaymentIntents and NicePay test approvals.",
            "Idempotency records and webhook event deduplication help prevent duplicate payment state changes.",
            "Audit logs and outbox records make approvals, refunds, cancellations, and webhook acknowledgements easier to trace.",
            "Operator pages and JSON APIs support transaction search, audit inspection, outbox review, webhook review, and machine readable exports."
        ));
        model.addAttribute("milestones", List.of(
            "Checkout entry that opens Stripe and NicePay public test routes.",
            "Stripe browser checkout with a public result page and webhook confirmation.",
            "NicePay test payment flow with public approval and operator protected cancellation.",
            "GitHub Actions runs automated tests before the Docker Compose stack is deployed on AWS Graviton EC2 with PostgreSQL on the same instance."
        ));
        return "home/index";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
