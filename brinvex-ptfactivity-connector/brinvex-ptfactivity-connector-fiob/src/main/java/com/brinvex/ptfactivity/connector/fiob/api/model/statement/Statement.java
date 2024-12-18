package com.brinvex.ptfactivity.connector.fiob.api.model.statement;


import com.brinvex.finance.types.vo.Money;

import java.time.LocalDate;
import java.util.List;

public sealed interface Statement {

    sealed interface TransStatement extends Statement {
        String accountId();

        LocalDate periodFrom();

        LocalDate periodTo();
    }

    record TradingTransStatement(
            @Override
            String accountId,
            @Override
            LocalDate periodFrom,
            @Override
            LocalDate periodTo,
            List<TradingTransaction> transactions,
            Lang lang
    ) implements TransStatement {
    }

    record SavingTransStatement(
            @Override
            String accountId,
            @Override
            LocalDate periodFrom,
            @Override
            LocalDate periodTo,
            List<SavingTransaction> transactions
    ) implements TransStatement {
    }

    record TradingSnapshotStatement(
            @Override
            LocalDate date,
            @Override
            Money nav
    ) implements Statement {
    }

}
