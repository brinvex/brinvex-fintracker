package com.brinvex.fintracker.connector.ibkr.api.factory;

import com.brinvex.fintracker.api.facade.HttpClientFacade;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.util.dms.api.Dms;

public interface IbkrFactory {

    IbkrStatementMerger statementMerger();

    IbkrStatementParser statementParser();

    IbkrFinTransactionMapper finTransactionMapper();

    IbkrDms dms(Dms dms);

    IbkrFetcher fetcher(HttpClientFacade httpClientFacade);

    IbkrPtfProgressProvider ptfProgressProvider(
            IbkrDms dms,
            IbkrStatementParser parser,
            IbkrFetcher fetcher,
            IbkrStatementMerger statementMerger,
            IbkrFinTransactionMapper transactionMapper
    );

    IbkrPtfProgressProvider ptfProgressProvider(Dms dms, HttpClientFacade httpClientFacade);

    static IbkrFactory create() {
        try {
            return (IbkrFactory) Class.forName("com.brinvex.fintracker.connector.ibkr.impl.factory.IbkrFactoryImpl")
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
