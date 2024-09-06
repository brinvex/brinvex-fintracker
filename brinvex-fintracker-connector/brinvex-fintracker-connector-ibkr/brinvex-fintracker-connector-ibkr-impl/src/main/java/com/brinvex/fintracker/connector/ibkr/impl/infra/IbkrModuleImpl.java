package com.brinvex.fintracker.connector.ibkr.impl.infra;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.FinTrackerModule;
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
import com.brinvex.util.dms.api.Dms;

public class IbkrModuleImpl implements IbkrModule, FinTrackerModule.ApplicationAware {

    private FinTracker finTracker;

    @Override
    public IbkrStatementMerger statementMerger() {
        return finTracker.get(IbkrStatementMerger.class, IbkrStatementMergerImpl::new);
    }

    @Override
    public IbkrStatementParser statementParser() {
        return finTracker.get(IbkrStatementParser.class, IbkrStatementParserImpl::new);
    }

    @Override
    public IbkrFinTransactionMapper finTransactionMapper() {
        return finTracker.get(IbkrFinTransactionMapper.class, IbkrFinTransactionMapperImpl::new);
    }

    @Override
    public IbkrDms dms() {
        return finTracker.get(IbkrDms.class, () -> {
            String dmsWorkspace = finTracker.property(IbkrModule.PROP_DMS_WORKSPACE, "ibkr");
            Dms dms = finTracker.dmsFactory().getDms(dmsWorkspace);
            return new IbkrDmsImpl(dms);
        });
    }

    @Override
    public IbkrFetcher fetcher() {
        return finTracker.get(IbkrFetcher.class, () -> new IbkrFetcherImpl(finTracker.httpClientFacade()));
    }

    @Override
    public IbkrPtfProgressProvider ptfProgressProvider() {
        return finTracker.get(IbkrPtfProgressProvider.class, () -> new IbkrPtfProgressProviderImpl(
                dms(),
                statementParser(),
                fetcher(),
                statementMerger(),
                finTransactionMapper()
        ));
    }

    @Override
    public void setFinTracker(FinTracker finTracker) {
        this.finTracker = finTracker;
    }
}
