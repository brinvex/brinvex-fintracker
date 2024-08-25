package com.brinvex.fintracker.api.exception;

public class AssistanceRequiredException extends IllegalStateException {

    public AssistanceRequiredException(String message) {
        super(message);
    }

    public AssistanceRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
