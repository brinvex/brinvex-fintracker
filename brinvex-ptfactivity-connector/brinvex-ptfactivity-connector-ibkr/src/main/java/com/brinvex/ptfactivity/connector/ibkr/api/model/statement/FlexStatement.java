package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public sealed interface FlexStatement permits FlexStatement.ActivityStatement, FlexStatement.TradeConfirmStatement {

    record ActivityStatement(
            String accountId,
            LocalDate fromDate,
            LocalDate toDate,
            ZonedDateTime whenGenerated,
            List<CashTransaction> cashTransactions,
            List<Trade> trades,
            List<CorporateAction> corporateActions,
            List<EquitySummary> equitySummaries
    ) implements FlexStatement {

        public ActivityStatement(
                String accountId,
                LocalDate fromDate,
                LocalDate toDate,
                ZonedDateTime whenGenerated,
                List<CashTransaction> cashTransactions,
                List<Trade> trades,
                List<CorporateAction> corporateActions,
                List<EquitySummary> equitySummaries
        ) {
            this.accountId = accountId;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.whenGenerated = whenGenerated;
            this.cashTransactions = cashTransactions == null ? null : List.copyOf(cashTransactions);
            this.trades = trades == null ? null : List.copyOf(trades);
            this.corporateActions = corporateActions == null ? null : List.copyOf(corporateActions);
            this.equitySummaries = equitySummaries == null ? null : List.copyOf(equitySummaries);
        }
    }

    record TradeConfirmStatement(
            String accountId,
            LocalDate fromDate,
            LocalDate toDate,
            ZonedDateTime whenGenerated,
            List<TradeConfirm> tradeConfirmations
    ) implements FlexStatement {
        public TradeConfirmStatement(
                String accountId,
                LocalDate fromDate,
                LocalDate toDate,
                ZonedDateTime whenGenerated,
                List<TradeConfirm> tradeConfirmations
        ) {
            this.accountId = accountId;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.whenGenerated = whenGenerated;
            this.tradeConfirmations = tradeConfirmations == null ? null : List.copyOf(tradeConfirmations);
        }
    }
}