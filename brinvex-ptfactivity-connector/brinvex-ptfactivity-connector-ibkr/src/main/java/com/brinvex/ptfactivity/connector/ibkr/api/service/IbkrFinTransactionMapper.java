package com.brinvex.ptfactivity.connector.ibkr.api.service;

import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.Trade;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeConfirm;

import java.util.List;

public interface IbkrFinTransactionMapper {

    List<FinTransaction> mapCashTransactions(List<CashTransaction> cashTrans);

    List<FinTransaction> mapTrades(List<Trade> trades);

    List<FinTransaction> mapCorporateAction(List<CorporateAction> corpActions);

    List<FinTransaction> mapTradeConfirms(List<TradeConfirm> tradeConfirms);

}
