package com.brinvex.fintracker.core.api.model.general;

public sealed interface Result<VALUE> {

    boolean isOk();

    VALUE value();

    String failReason();

    static <VALUE> Ok<VALUE> ok(VALUE value) {
        return new Ok<>(value);
    }

    static <VALUE> Fail<VALUE> fail(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        return new Fail<>(reason);
    }

    record Ok<VALUE>(
            @Override
            VALUE value
    ) implements Result<VALUE> {

        @Override
        public String failReason() {
            return null;
        }

        @Override
        public boolean isOk() {
            return true;
        }
    }

    record Fail<VALUE>(
            @Override
            String failReason
    ) implements Result<VALUE> {

        @Override
        public VALUE value() {
            throw new IllegalStateException(failReason);
        }

        @Override
        public boolean isOk() {
            return false;
        }
    }

}
