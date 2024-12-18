package com.brinvex.ptfactivity.connector.ibkr.api;

import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrDms;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrPtfActivityProvider;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.ptfactivity.core.api.Module;

public interface IbkrModule extends Module {

    IbkrFetcher fetcher();

    IbkrStatementParser statementParser();

    IbkrStatementMerger statementMerger();

    IbkrDms dms();

    IbkrFinTransactionMapper finTransactionMapper();

    IbkrPtfActivityProvider ptfProgressProvider();
}
