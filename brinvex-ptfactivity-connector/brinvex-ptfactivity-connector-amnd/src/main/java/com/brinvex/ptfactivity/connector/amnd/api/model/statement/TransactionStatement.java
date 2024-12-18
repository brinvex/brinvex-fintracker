package com.brinvex.ptfactivity.connector.amnd.api.model.statement;

import java.util.List;

public record TransactionStatement(
        String accountId,
        List<Trade> trades
) {
}
