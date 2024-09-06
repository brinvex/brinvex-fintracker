package com.brinvex.fintracker.core.api.exception;

public class FinTransactionProcessingException extends RuntimeException {

    public FinTransactionProcessingException(String message) {
        super(message);
    }

    public FinTransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
