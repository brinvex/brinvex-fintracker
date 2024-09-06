package com.brinvex.fintracker.core.api.provider;

public interface Provider<REQUEST, RESPONSE> {

    boolean supports(REQUEST request);

    RESPONSE process(REQUEST request);
}
