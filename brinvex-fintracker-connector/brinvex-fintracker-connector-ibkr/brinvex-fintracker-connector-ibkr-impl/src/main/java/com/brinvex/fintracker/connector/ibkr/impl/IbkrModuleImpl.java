package com.brinvex.fintracker.connector.ibkr.impl;

import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrDmsImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrFetcherImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrFinTransactionMapperImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrPtfProgressProviderImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrStatementMergerImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrStatementParserImpl;
import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleContext;

public class IbkrModuleImpl implements IbkrModule, FinTrackerModule {

    private final FinTrackerModuleContext finTrackerCtx;

    public IbkrModuleImpl(FinTrackerModuleContext finTrackerCtx) {
        this.finTrackerCtx = finTrackerCtx;
    }

    @Override
    public IbkrStatementMerger statementMerger() {
        return finTrackerCtx.singletonService(IbkrStatementMerger.class, IbkrStatementMergerImpl::new);
    }

    @Override
    public IbkrStatementParser statementParser() {
        return finTrackerCtx.singletonService(IbkrStatementParser.class, IbkrStatementParserImpl::new);
    }

    @Override
    public IbkrFinTransactionMapper finTransactionMapper() {
        return finTrackerCtx.singletonService(IbkrFinTransactionMapper.class, IbkrFinTransactionMapperImpl::new);
    }

    @Override
    public IbkrDms dms() {
        return finTrackerCtx.singletonService(IbkrDms.class, () -> new IbkrDmsImpl(finTrackerCtx.dms()));
    }

    @Override
    public IbkrFetcher fetcher() {
        return finTrackerCtx.singletonService(IbkrFetcher.class, () -> new IbkrFetcherImpl(finTrackerCtx.httpClientFacade()));
    }

    @Override
    public IbkrPtfProgressProvider ptfProgressProvider() {
        return finTrackerCtx.singletonService(IbkrPtfProgressProvider.class, () -> new IbkrPtfProgressProviderImpl(
                dms(),
                statementParser(),
                fetcher(),
                statementMerger(),
                finTransactionMapper(),
                finTrackerCtx.validator()
        ));
    }
}
