package com.brinvex.ptfactivity.connector.fiob.api.service;


import com.brinvex.ptfactivity.connector.fiob.api.model.FiobAccountType;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.SavingTransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.SnapshotDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingSnapshotDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingTransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TransDocKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FiobDms {

    List<TradingTransDocKey> getTradingTransDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl);

    List<TradingSnapshotDocKey> getTradingSnapshotDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl);

    List<SavingTransDocKey> getSavingTransDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl);

    default List<? extends TransDocKey> getTransDocKeys(String accountId, FiobAccountType accountType, LocalDate fromDateIncl, LocalDate toDateIncl) {
        return switch (accountType) {
            case SAVING -> getSavingTransDocKeys(accountId, fromDateIncl, toDateIncl);
            case TRADING -> getTradingTransDocKeys(accountId, fromDateIncl, toDateIncl);
        };
    }

    default List<? extends SnapshotDocKey> getSnapshotDocKeys(String accountId, FiobAccountType accountType, LocalDate fromDateIncl, LocalDate toDateIncl) {
        return switch (accountType) {
            case SAVING -> null;
            case TRADING -> getTradingSnapshotDocKeys(accountId, fromDateIncl, toDateIncl);
        };
    }

    String getStatementContent(FiobDocKey docKey);

    List<String> getStatementContentLinesIfExists(FiobDocKey docKey, int limit);

    LocalDateTime getStatementLastModifiedTimeIfExists(FiobDocKey docKey);

    boolean putStatement(FiobDocKey docKey, String content);

    void delete(FiobDocKey docKey);

}
