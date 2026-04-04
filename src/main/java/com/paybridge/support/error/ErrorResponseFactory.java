package com.paybridge.support.error;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseFactory {

    public ApiErrorResponse create(
            HttpStatus status,
            ErrorCode code,
            String message,
            String path,
            String correlationId,
            List<ApiFieldViolation> fieldViolations
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                correlationId,
                fieldViolations == null ? List.of() : List.copyOf(fieldViolations)
        );
    }
}
