package com.brinvex.fintracker.connector.ibkr.api.service;

import com.brinvex.fintracker.api.FinTrackerModule;

public interface IbkrModule extends FinTrackerModule {

    String PROP_DMS_WORKSPACE = "connector.ibkr.dms.workspace";

    IbkrStatementMerger statementMerger();

    IbkrStatementParser statementParser();

    IbkrFinTransactionMapper finTransactionMapper();

    IbkrDms dms();

    IbkrFetcher fetcher();

    IbkrPtfProgressProvider ptfProgressProvider();
}
