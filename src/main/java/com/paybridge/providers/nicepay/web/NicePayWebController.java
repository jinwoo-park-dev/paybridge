package com.paybridge.providers.nicepay.web;

import com.paybridge.providers.nicepay.NicePayApprovalApplicationService;
import com.paybridge.providers.nicepay.NicePayApprovalOutcome;
import com.paybridge.providers.nicepay.NicePayCancellationApplicationService;
import com.paybridge.providers.nicepay.NicePayCancellationOutcome;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.PayBridgeException;
import io.swagger.v3.oas.annotations.Hidden;
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

@Hidden
@Controller
@RequestMapping("/payments/nicepay")
public class NicePayWebController {

    private final NicePayApprovalApplicationService nicePayApprovalApplicationService;
    private final NicePayCancellationApplicationService nicePayCancellationApplicationService;
    private final PayBridgeProperties payBridgeProperties;

    public NicePayWebController(
            NicePayApprovalApplicationService nicePayApprovalApplicationService,
            NicePayCancellationApplicationService nicePayCancellationApplicationService,
            PayBridgeProperties payBridgeProperties
    ) {
        this.nicePayApprovalApplicationService = nicePayApprovalApplicationService;
        this.nicePayCancellationApplicationService = nicePayCancellationApplicationService;
        this.payBridgeProperties = payBridgeProperties;
    }

    @GetMapping("/keyin")
    public String renderKeyInPage(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Long amountMinor,
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String buyerName,
            @RequestParam(required = false) String buyerEmail,
            @RequestParam(required = false) String buyerTel,
            Model model
    ) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", NicePayKeyInForm.seeded(orderId, amountMinor, goodsName, buyerName, buyerEmail, buyerTel));
        }
        populatePageModel(model);
        return "payment/nicepay-keyin";
    }

    @PostMapping("/keyin/approve")
    public String approve(
            @Valid @ModelAttribute("form") NicePayKeyInForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            form.clearSensitiveFields();
            populatePageModel(model);
            return "payment/nicepay-keyin";
        }

        try {
            NicePayApprovalOutcome outcome = nicePayApprovalApplicationService.approve(form.toCommand());
            redirectAttributes.addFlashAttribute(
                    "opsMessage",
                    outcome.replayed()
                            ? "Duplicate submission detected. The existing NicePay transaction record was reused."
                            : "NicePay key-in approval succeeded. TID: " + outcome.tid()
            );
            return "redirect:/payments/" + outcome.paymentId();
        } catch (PayBridgeException ex) {
            form.clearSensitiveFields();
            model.addAttribute("submitError", ex.getMessage());
            populatePageModel(model);
            return "payment/nicepay-keyin";
        }
    }

    @PostMapping("/{paymentId}/cancel")
    public String fullCancel(
            @PathVariable UUID paymentId,
            @Valid @ModelAttribute("fullCancelForm") NicePayFullCancelForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("opsError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/payments/" + paymentId;
        }

        NicePayCancellationOutcome outcome = nicePayCancellationApplicationService.cancelFully(paymentId, form.getCancelReason());
        redirectAttributes.addFlashAttribute("opsMessage", "NicePay full cancellation completed for payment " + outcome.paymentId() + ".");
        return "redirect:/payments/" + paymentId;
    }

    @PostMapping("/{paymentId}/partial-cancel")
    public String partialCancel(
            @PathVariable UUID paymentId,
            @Valid @ModelAttribute("partialCancelForm") NicePayPartialCancelForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("opsError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/payments/" + paymentId;
        }

        NicePayCancellationOutcome outcome = nicePayCancellationApplicationService.cancelPartially(
                paymentId,
                form.getCancelAmountMinor(),
                form.getCancelReason()
        );
        redirectAttributes.addFlashAttribute("opsMessage", "NicePay partial cancellation completed for payment " + outcome.paymentId() + ".");
        return "redirect:/payments/" + paymentId;
    }

    private void populatePageModel(Model model) {
        PayBridgeProperties.NicePay provider = payBridgeProperties.getProviders().getNicepay();
        boolean nicePayEnabled = payBridgeProperties.getFeatures().isNicepayEnabled() && provider.isEnabled();
        model.addAttribute("nicePayEnabled", nicePayEnabled);
        model.addAttribute("nicePayLocalOnly", payBridgeProperties.getFeatures().isNicepayLocalOnly());
        model.addAttribute("merchantConfigured", provider.getMerchantId() != null && !provider.getMerchantId().isBlank());
        model.addAttribute("merchantIdPreview", provider.getMerchantId() == null || provider.getMerchantId().isBlank() ? "(not configured)" : provider.getMerchantId());
        model.addAttribute("testWarnings", List.of(
                "NicePay key-in uses merchant-hosted card entry, so keep this route authenticated and local only.",
                "Approval requests are posted to NicePay using EUC-KR form encoding.",
                "Partial cancellation on the test MID can temporarily hold the remaining card limit until it is released in the merchant portal."
        ));
        model.addAttribute("providerChecklist", List.of(
                "Store the MID and merchant key in local environment variables only.",
                "Do not persist raw PAN, buyer auth number, or card password digits.",
                "Use this route to exercise approval, full cancellation, and partial cancellation."
        ));
    }
}
