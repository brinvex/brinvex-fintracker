package com.brinvex.ptfactivity.connector.amnd.api.service;

import com.brinvex.ptfactivity.connector.amnd.api.model.statement.Trade;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;

import java.util.List;

public interface AmndFinTransactionMapper {

    List<FinTransaction.FinTransactionBuilder> mapTradeToFinTransactionPair(Trade trade);

}
