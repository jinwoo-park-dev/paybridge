package com.paybridge.web.system;

import com.paybridge.support.config.PayBridgeProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "Small JSON endpoint used to verify runtime metadata and active profile state.")
@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private static final String RELEASE_VERSION = "0.1.0";

    private final Environment environment;
    private final PayBridgeProperties payBridgeProperties;

    public SystemInfoController(Environment environment, PayBridgeProperties payBridgeProperties) {
        this.environment = environment;
        this.payBridgeProperties = payBridgeProperties;
    }

    @Operation(
            summary = "Get service metadata",
            description = "Returns basic runtime metadata used by local smoke tests and environment verification.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "System metadata returned.",
                            content = @Content(schema = @Schema(implementation = SystemInfoResponse.class)))
            }
    )
    @GetMapping("/info")
    public SystemInfoResponse info() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return new SystemInfoResponse(
                "paybridge",
                "PayBridge — Payment Orchestration Service",
                RELEASE_VERSION,
                payBridgeProperties.getApp().getUiStrategy(),
                payBridgeProperties.getApp().getArchitectureStyle(),
                payBridgeProperties.getFeatures().isUnifiedCheckoutEnabled(),
                payBridgeProperties.getFeatures().isOperatorApiEnabled(),
                activeProfiles
        );
    }
}
