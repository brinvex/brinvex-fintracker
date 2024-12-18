package com.brinvex.ptfactivity.core.api.exception;

public class AssistanceRequiredException extends IllegalStateException {

    public AssistanceRequiredException(String message) {
        super(message);
    }

    public AssistanceRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
