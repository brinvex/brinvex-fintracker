package com.brinvex.fintracker.connector.ibkr.api.model;

import java.io.Serializable;

public record IbkrCredentials(
        String token,
        String activityFlexQueryId,
        String tradeConfirmFlexQueryId
) implements Serializable {
}