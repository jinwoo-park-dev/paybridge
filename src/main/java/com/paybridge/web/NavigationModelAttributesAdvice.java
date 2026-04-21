package com.paybridge.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Hidden
@ControllerAdvice
public class NavigationModelAttributesAdvice {

    private final boolean apiReferenceEnabled;

    public NavigationModelAttributesAdvice(@Value("${springdoc.swagger-ui.enabled:true}") boolean apiReferenceEnabled) {
        this.apiReferenceEnabled = apiReferenceEnabled;
    }

    @ModelAttribute("operatorAuthenticated")
    boolean operatorAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("operatorLoginUrl")
    String operatorLoginUrl() {
        return "/operator/login";
    }

    @ModelAttribute("apiReferenceEnabled")
    boolean apiReferenceEnabled() {
        return apiReferenceEnabled;
    }
}
