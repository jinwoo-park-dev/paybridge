package com.paybridge.support.error;

public record ApiFieldViolation(
        String field,
        String message,
        String rejectedValue
) {
}
