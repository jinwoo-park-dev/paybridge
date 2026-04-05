package com.paybridge.operator.web;

import com.paybridge.support.config.PayBridgeProperties;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Hidden
@Controller
public class OperatorLoginController {

    private final PayBridgeProperties payBridgeProperties;

    public OperatorLoginController(PayBridgeProperties payBridgeProperties) {
        this.payBridgeProperties = payBridgeProperties;
    }

    @GetMapping("/operator/login")
    public String login(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            Authentication authentication,
            Model model
    ) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/ops/transactions/search";
        }
        model.addAttribute("projectName", payBridgeProperties.getApp().getDisplayName());
        model.addAttribute("loginError", error != null);
        model.addAttribute("loggedOut", logout != null);
        model.addAttribute("nicepayOperatorOnly", payBridgeProperties.getFeatures().isNicepayLocalOnly());
        return "operator/login";
    }
}
