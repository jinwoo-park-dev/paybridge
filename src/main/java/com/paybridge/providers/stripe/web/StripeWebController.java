package com.paybridge.providers.stripe.web;

import com.paybridge.providers.stripe.StripeCheckoutPageView;
import com.paybridge.providers.stripe.StripePaymentConfirmationOutcome;
import com.paybridge.providers.stripe.StripePaymentIntentApplicationService;
import com.paybridge.providers.stripe.StripePaymentResultView;
import com.paybridge.providers.stripe.StripeRefundApplicationService;
import com.paybridge.providers.stripe.StripeRefundOutcome;
import com.paybridge.support.config.PayBridgeProperties;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Hidden
@Controller
@RequestMapping("/payments/stripe")
public class StripeWebController {

    private final StripePaymentIntentApplicationService stripePaymentIntentApplicationService;
    private final StripeRefundApplicationService stripeRefundApplicationService;
    private final PayBridgeProperties payBridgeProperties;

    public StripeWebController(
            StripePaymentIntentApplicationService stripePaymentIntentApplicationService,
            StripeRefundApplicationService stripeRefundApplicationService,
            PayBridgeProperties payBridgeProperties
    ) {
        this.stripePaymentIntentApplicationService = stripePaymentIntentApplicationService;
        this.stripeRefundApplicationService = stripeRefundApplicationService;
        this.payBridgeProperties = payBridgeProperties;
    }

    @GetMapping("/checkout")
    public String renderCheckoutPage(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Long amountMinor,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String customerEmail,
            Model model,
            HttpServletRequest request
    ) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", StripeCheckoutForm.seeded(orderId, amountMinor, currency, description, customerEmail));
        }
        populatePageModel(model, null, request);
        return "payment/stripe-checkout";
    }

    @PostMapping("/payment-intent")
    public String createPaymentIntent(
            @Valid @ModelAttribute("form") StripeCheckoutForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request
    ) {
        if (bindingResult.hasErrors()) {
            populatePageModel(model, null, request);
            return "payment/stripe-checkout";
        }

        try {
            StripeCheckoutPageView checkout = stripePaymentIntentApplicationService.createCheckoutSession(form.toCommand());
            model.addAttribute("form", form);
            populatePageModel(model, checkout, request);
            return "payment/stripe-checkout";
        } catch (RuntimeException ex) {
            model.addAttribute("form", form);
            model.addAttribute("submitError", ex.getMessage());
            populatePageModel(model, null, request);
            return "payment/stripe-checkout";
        }
    }

    @GetMapping("/return")
    public String handleReturn(
            @RequestParam("payment_intent") String paymentIntentId,
            Model model
    ) {
        return renderPublicResult(paymentIntentId, model);
    }

    @GetMapping("/result")
    public String renderResult(
            @RequestParam("payment_intent") String paymentIntentId,
            Model model
    ) {
        return renderPublicResult(paymentIntentId, model);
    }

    private String renderPublicResult(String paymentIntentId, Model model) {
        try {
            StripePaymentConfirmationOutcome outcome = stripePaymentIntentApplicationService.confirmAndRecord(paymentIntentId);
            model.addAttribute("result", StripePaymentResultView.success(outcome));
        } catch (RuntimeException ex) {
            model.addAttribute("result", StripePaymentResultView.failure(paymentIntentId, ex.getMessage()));
        }
        return "payment/stripe-result";
    }

    @PostMapping("/{paymentId}/refund")
    public String fullRefund(
            @PathVariable UUID paymentId,
            @Valid @ModelAttribute("stripeFullRefundForm") StripeFullRefundForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("opsError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/payments/" + paymentId;
        }
        StripeRefundOutcome outcome = stripeRefundApplicationService.refundFully(paymentId, form.getRefundReason());
        redirectAttributes.addFlashAttribute("opsMessage", "Stripe full refund completed. Refund ID: " + outcome.refundId());
        return "redirect:/payments/" + paymentId;
    }

    @PostMapping("/{paymentId}/partial-refund")
    public String partialRefund(
            @PathVariable UUID paymentId,
            @Valid @ModelAttribute("stripePartialRefundForm") StripePartialRefundForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("opsError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/payments/" + paymentId;
        }
        StripeRefundOutcome outcome = stripeRefundApplicationService.refundPartially(paymentId, form.getRefundAmountMinor(), form.getRefundReason());
        redirectAttributes.addFlashAttribute("opsMessage", "Stripe partial refund completed. Refund ID: " + outcome.refundId());
        return "redirect:/payments/" + paymentId;
    }

    private void populatePageModel(Model model, StripeCheckoutPageView checkout, HttpServletRequest request) {
        PayBridgeProperties.Stripe provider = payBridgeProperties.getProviders().getStripe();
        boolean stripeEnabled = payBridgeProperties.getFeatures().isStripeEnabled() && provider.isEnabled();
        String publishableKey = provider.getPublishableKey();
        model.addAttribute("stripeEnabled", stripeEnabled);
        model.addAttribute("publishableKeyConfigured", publishableKey != null && !publishableKey.isBlank());
        model.addAttribute("publishableKeyPreview", previewKey(publishableKey));
        model.addAttribute("returnUrl", buildReturnUrl(request));
        model.addAttribute("testWarnings", List.of(
                "The backend creates the PaymentIntent and records the payment; Stripe.js only handles secure in-browser confirmation.",
                "Use a fresh demo order ID for each new payment attempt to avoid replaying an already completed PaymentIntent.",
                "Webhook verification also requires PAYBRIDGE_STRIPE_WEBHOOK_SECRET."
        ));
        model.addAttribute("providerChecklist", List.of(
                "Use Stripe test mode for local runs and public demos.",
                "Keep the publishable key in the browser and the secret key only in server-side configuration.",
                "Return-page verification and webhook processing should converge on the same payment record."
        ));
        if (checkout != null) {
            model.addAttribute("checkout", checkout);
            model.addAttribute("stripePublishableKey", publishableKey == null ? "" : publishableKey);
        }
    }

    private String previewKey(String publishableKey) {
        if (publishableKey == null || publishableKey.isBlank()) {
            return "(not configured)";
        }
        if (publishableKey.length() <= 12) {
            return publishableKey;
        }
        return publishableKey.substring(0, 12) + "...";
    }

    private String buildReturnUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request)
                .path("/payments/stripe/return")
                .build()
                .toUriString();
    }
}
