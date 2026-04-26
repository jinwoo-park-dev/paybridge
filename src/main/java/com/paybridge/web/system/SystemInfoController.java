package com.paybridge.web.system;

import com.paybridge.support.config.PayBridgeProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "Small JSON endpoint used for public demo smoke checks.")
@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private static final String RELEASE_VERSION = "1.0.0";

    private final PayBridgeProperties payBridgeProperties;

    public SystemInfoController(PayBridgeProperties payBridgeProperties) {
        this.payBridgeProperties = payBridgeProperties;
    }

    @Operation(
        summary = "Get public service metadata",
        description = "Returns minimal metadata for public smoke checks without exposing runtime profiles or internal implementation labels.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Public service metadata returned.",
                content = @Content(schema = @Schema(implementation = SystemInfoResponse.class)))
        }
    )
    @GetMapping("/info")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(
            "paybridge",
            payBridgeProperties.getApp().getDisplayName(),
            RELEASE_VERSION,
            "Public demo for test payment flows",
            List.of(
                "Home page",
                "Checkout selection",
                "Stripe test checkout",
                "NicePay test payment form",
                "Public payment result pages",
                "Health check"
            ),
            List.of(
                "Transaction search",
                "Payment detail",
                "Refund and cancellation actions",
                "Audit, outbox, webhook, and export APIs"
            )
        );
    }
}
