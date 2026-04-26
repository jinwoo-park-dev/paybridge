package com.paybridge.web.system;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "SystemInfoResponse", description = "Minimal metadata returned by the public system info endpoint.")
public record SystemInfoResponse(
    @Schema(example = "paybridge")
    String service,
    @Schema(example = "PayBridge")
    String project,
    @Schema(example = "1.0.0")
    String releaseVersion,
    @Schema(example = "Public demo for test payment flows")
    String demoType,
    @ArraySchema(schema = @Schema(example = "Checkout selection"))
    List<String> publicSurfaces,
    @ArraySchema(schema = @Schema(example = "Payment detail"))
    List<String> protectedSurfaces
) {
}
