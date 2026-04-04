package com.paybridge.support.error;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(name = "ApiErrorResponse", description = "Common structured API error response.")
public record ApiErrorResponse(
        @Schema(format = "date-time")
        Instant timestamp,
        int status,
        String error,
        ErrorCode code,
        String message,
        String path,
        String correlationId,
        @ArraySchema(schema = @Schema(implementation = ApiFieldViolation.class))
        List<ApiFieldViolation> fieldViolations
) {
}
