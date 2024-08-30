package com.brinvex.fintracker.connector.ibkr.impl.infra;

import com.brinvex.fintracker.api.FinTracker;
import com.brinvex.fintracker.api.FinTrackerModule;
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

    private FinTracker application;

    @Override
    public IbkrStatementMerger statementMerger() {
        return application.get(IbkrStatementMerger.class, IbkrStatementMergerImpl::new);
    }

    @Override
    public IbkrStatementParser statementParser() {
        return application.get(IbkrStatementParser.class, IbkrStatementParserImpl::new);
    }

    @Override
    public IbkrFinTransactionMapper finTransactionMapper() {
        return application.get(IbkrFinTransactionMapper.class, IbkrFinTransactionMapperImpl::new);
    }

    @Override
    public IbkrDms dms() {
        return application.get(IbkrDms.class, () -> {
            String dmsWorkspace = application.property(IbkrModule.PROP_DMS_WORKSPACE, "ibkr");
            Dms dms = application.dmsFactory().getDms(dmsWorkspace);
            return new IbkrDmsImpl(dms);
        });
    }

    @Override
    public IbkrFetcher fetcher() {
        return application.get(IbkrFetcher.class, () -> new IbkrFetcherImpl(application.httpClientFacade()));
    }

    @Override
    public IbkrPtfProgressProvider ptfProgressProvider() {
        return application.get(IbkrPtfProgressProvider.class, () -> new IbkrPtfProgressProviderImpl(
                dms(),
                statementParser(),
                fetcher(),
                statementMerger(),
                finTransactionMapper()
        ));
    }

    @Override
    public void setApplication(FinTracker application) {
        this.application = application;
    }
}
