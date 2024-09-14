package com.brinvex.fintracker.connector.ibkr.api;

import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.fintracker.core.api.internal.FinTrackerModule;

public interface IbkrModule extends FinTrackerModule {

    IbkrStatementMerger statementMerger();

    IbkrStatementParser statementParser();

    IbkrFinTransactionMapper finTransactionMapper();

    IbkrDms dms();

    IbkrFetcher fetcher();

    IbkrPtfProgressProvider ptfProgressProvider();
}
