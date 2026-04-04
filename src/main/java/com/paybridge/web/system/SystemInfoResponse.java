package com.paybridge.web.system;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "SystemInfoResponse", description = "Runtime metadata returned by the system info endpoint.")
public record SystemInfoResponse(
        @Schema(example = "paybridge")
        String service,
        @Schema(example = "PayBridge — Payment Orchestration Service")
        String project,
        @Schema(example = "0.1.0")
        String releaseVersion,
        @Schema(example = "Spring MVC + server-rendered pages + minimal JS")
        String frontendChoice,
        @Schema(example = "Modular Monolith")
        String architectureStyle,
        boolean unifiedCheckoutEnabled,
        boolean operatorApiEnabled,
        List<String> activeProfiles
) {
}
