package com.paybridge.providers.nicepay.web;

import com.paybridge.payment.application.PaymentDetailView;
import com.paybridge.payment.application.PaymentQueryApplicationService;
import com.paybridge.payment.domain.PaymentProvider;
import com.paybridge.providers.nicepay.NicePayApprovalApplicationService;
import com.paybridge.providers.nicepay.NicePayApprovalOutcome;
import com.paybridge.providers.nicepay.NicePayCancellationApplicationService;
import com.paybridge.providers.nicepay.NicePayCancellationOutcome;
import com.paybridge.support.config.PayBridgeProperties;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
    private final PaymentQueryApplicationService paymentQueryApplicationService;
    private final PayBridgeProperties payBridgeProperties;

    public NicePayWebController(
            NicePayApprovalApplicationService nicePayApprovalApplicationService,
            NicePayCancellationApplicationService nicePayCancellationApplicationService,
            PaymentQueryApplicationService paymentQueryApplicationService,
            PayBridgeProperties payBridgeProperties
    ) {
        this.nicePayApprovalApplicationService = nicePayApprovalApplicationService;
        this.nicePayCancellationApplicationService = nicePayCancellationApplicationService;
        this.paymentQueryApplicationService = paymentQueryApplicationService;
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

        if (!nicePayReadyForApproval()) {
            form.clearSensitiveFields();
            model.addAttribute("submitError", "NicePay test approval is unavailable until the feature flag, provider credentials, MID, and merchant key are configured.");
            populatePageModel(model);
            return "payment/nicepay-keyin";
        }

        try {
            NicePayApprovalOutcome outcome = nicePayApprovalApplicationService.approve(form.toCommand());
            redirectAttributes.addAttribute("paymentId", outcome.paymentId());
            redirectAttributes.addAttribute("replayed", outcome.replayed());
            form.clearSensitiveFields();
            return "redirect:/payments/nicepay/result";
        } catch (PayBridgeException ex) {
            form.clearSensitiveFields();
            model.addAttribute("submitError", ex.getMessage());
            populatePageModel(model);
            return "payment/nicepay-keyin";
        }
    }

    @GetMapping("/result")
    public String approvalResult(
            @RequestParam UUID paymentId,
            @RequestParam(defaultValue = "false") boolean replayed,
            Model model
    ) {
        PaymentDetailView detail = paymentQueryApplicationService.getDetail(paymentId);
        if (detail.provider() != PaymentProvider.NICEPAY) {
            throw new PayBridgeException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "NicePay payment not found: " + paymentId
            );
        }
        model.addAttribute("result", NicePayApprovalResultView.from(detail, replayed));
        return "payment/nicepay-result";
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
        boolean nicePayReady = nicePayReadyForApproval();
        model.addAttribute("nicePayEnabled", nicePayReady);
        model.addAttribute("nicePayLocalOnly", payBridgeProperties.getFeatures().isNicepayLocalOnly());
        model.addAttribute("merchantConfigured", merchantConfigured(provider));
        model.addAttribute("merchantIdPreview", provider.getMerchantId() == null || provider.getMerchantId().isBlank() ? "(not configured)" : provider.getMerchantId());
        model.addAttribute("testWarnings", List.of(
                "NicePay key-in is open for public test demos, but it must use provider test credentials and approved test card data only.",
                "Approval requests are posted to NicePay using EUC-KR form encoding.",
                "Do not enter real card numbers, real birth dates, real business numbers, or real personal information.",
                "Partial cancellation on the test MID can temporarily hold the remaining card limit until it is released in the merchant portal."
        ));
        model.addAttribute("providerChecklist", List.of(
                "Store the MID and merchant key in environment variables or SSM only.",
                "PayBridge does not persist raw PAN, buyer auth number, or card password digits.",
                "Use this route to exercise approval, full cancellation, and partial cancellation with test credentials."
        ));
    }

    private boolean nicePayReadyForApproval() {
        PayBridgeProperties.NicePay provider = payBridgeProperties.getProviders().getNicepay();
        return payBridgeProperties.getFeatures().isNicepayEnabled()
                && provider.isEnabled()
                && merchantConfigured(provider);
    }

    private boolean merchantConfigured(PayBridgeProperties.NicePay provider) {
        return hasText(provider.getMerchantId()) && hasText(provider.getMerchantKey());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
