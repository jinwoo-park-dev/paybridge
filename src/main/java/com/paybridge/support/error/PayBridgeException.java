package com.paybridge.support.error;

import org.springframework.http.HttpStatus;

public class PayBridgeException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    public PayBridgeException(HttpStatus status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }
}
