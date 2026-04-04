package com.paybridge.support.error;

import com.paybridge.payment.domain.PaymentDomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    private final ErrorResponseFactory errorResponseFactory;

    public GlobalExceptionHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(PayBridgeException.class)
    public Object handlePayBridgeException(PayBridgeException ex, HttpServletRequest request) {
        return render(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(PaymentDomainException.class)
    public Object handlePaymentDomainException(PaymentDomainException ex, HttpServletRequest request) {
        return render(HttpStatus.CONFLICT, ErrorCode.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return render(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                "The request conflicts with existing payment data or a concurrent update.",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiFieldViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toViolation)
                .toList();

        return render(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Request validation failed.",
                request,
                violations
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnhandledException(Exception ex, HttpServletRequest request) {
        return render(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred.",
                request,
                List.of()
        );
    }

    private Object render(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request,
            List<ApiFieldViolation> fieldViolations
    ) {
        String path = request.getRequestURI();
        String correlationId = (String) request.getAttribute(CORRELATION_ID_ATTRIBUTE);

        if (path.startsWith("/api/")) {
            ApiErrorResponse response = errorResponseFactory.create(
                    status,
                    code,
                    message,
                    path,
                    correlationId,
                    fieldViolations
            );
            return ResponseEntity.status(status).body(response);
        }

        ModelAndView modelAndView = new ModelAndView("error/default-error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("error", status.getReasonPhrase());
        modelAndView.addObject("code", code.name());
        modelAndView.addObject("message", message);
        modelAndView.addObject("path", path);
        modelAndView.addObject("correlationId", correlationId);
        modelAndView.addObject("fieldViolations", fieldViolations);
        return modelAndView;
    }

    private ApiFieldViolation toViolation(FieldError fieldError) {
        Object rejectedValue = fieldError.getRejectedValue();
        return new ApiFieldViolation(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                rejectedValue == null ? null : String.valueOf(rejectedValue)
        );
    }
}
