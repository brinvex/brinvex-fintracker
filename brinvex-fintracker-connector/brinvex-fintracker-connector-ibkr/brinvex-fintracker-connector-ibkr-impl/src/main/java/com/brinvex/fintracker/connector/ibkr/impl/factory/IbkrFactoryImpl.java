package com.brinvex.fintracker.connector.ibkr.impl.factory;

import com.brinvex.fintracker.api.facade.HttpClientFacade;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.factory.IbkrFactory;
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
import com.brinvex.util.java.LazyConstant;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class IbkrFactoryImpl implements IbkrFactory {

    private final LazyConstant<IbkrStatementMerger> statementMerger = LazyConstant.threadSafe(IbkrStatementMergerImpl::new);

    private final LazyConstant<IbkrStatementParser> statementParser = LazyConstant.threadSafe(IbkrStatementParserImpl::new);

    private final LazyConstant<IbkrFinTransactionMapper> transactionMapper = LazyConstant.threadSafe(IbkrFinTransactionMapperImpl::new);

    private final ConcurrentHashMap<Dms, IbkrDms> dmsInstances = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<HttpClientFacade, IbkrFetcher> fetcherInstances = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<List<Object>, IbkrPtfProgressProvider> ptfProgressProviderInstances = new ConcurrentHashMap<>();

    @Override
    public IbkrStatementMerger statementMerger() {
        return statementMerger.get();
    }

    @Override
    public IbkrStatementParser statementParser() {
        return statementParser.get();
    }

    @Override
    public IbkrFinTransactionMapper finTransactionMapper() {
        return transactionMapper.get();
    }

    @Override
    public IbkrDms dms(Dms dms) {
        return this.dmsInstances.computeIfAbsent(dms, _ -> new IbkrDmsImpl(dms));
    }

    @Override
    public IbkrFetcher fetcher(HttpClientFacade httpClientFacade) {
        return this.fetcherInstances.computeIfAbsent(httpClientFacade, _ -> new IbkrFetcherImpl(httpClientFacade));
    }

    @Override
    public IbkrPtfProgressProvider ptfProgressProvider(
            IbkrDms dms,
            IbkrStatementParser parser,
            IbkrFetcher fetcher,
            IbkrStatementMerger statementMerger,
            IbkrFinTransactionMapper finTransactionMapper
    ) {
        return ptfProgressProviderInstances.computeIfAbsent(
                List.of(dms, parser, fetcher, statementMerger, finTransactionMapper), _ -> new IbkrPtfProgressProviderImpl(
                        dms,
                        parser,
                        fetcher,
                        statementMerger,
                        finTransactionMapper
                ));
    }

    @Override
    public IbkrPtfProgressProvider ptfProgressProvider(Dms dms, HttpClientFacade httpClientFacade) {
        IbkrDms ibkrDms = dms(dms);
        IbkrStatementParser parser = statementParser();
        IbkrFetcher fetcher = fetcher(httpClientFacade);
        IbkrStatementMerger statementMerger = statementMerger();
        IbkrFinTransactionMapper finTransactionMapper = finTransactionMapper();
        return ptfProgressProvider(ibkrDms, parser, fetcher, statementMerger, finTransactionMapper);
    }


}
