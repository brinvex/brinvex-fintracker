package com.brinvex.ptfactivity.connector.fiob.internal.service.mapper;

import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Lang;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.SavingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobFinTransactionMapper;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;

import java.util.List;

public class FiobFinTransactionMapperImpl implements FiobFinTransactionMapper {

    private final FiobTradingTransactionMapper fiobTradingTransMapper;

    private final FiobSavingTransactionMapper fiobSavingTransactionMapper;

    public FiobFinTransactionMapperImpl() {
        fiobTradingTransMapper = new FiobTradingTransactionMapper();
        fiobSavingTransactionMapper = new FiobSavingTransactionMapper();
    }

    @Override
    public List<FinTransaction> mapTradingTransactions(List<TradingTransaction> tradingTrans, Lang lang) {
        return fiobTradingTransMapper.mapTransactions(tradingTrans, lang);
    }

    @Override
    public List<FinTransaction> mapSavingTransactions(List<SavingTransaction> tradingTrans) {
        return fiobSavingTransactionMapper.mapTransactions(tradingTrans);
    }
}
