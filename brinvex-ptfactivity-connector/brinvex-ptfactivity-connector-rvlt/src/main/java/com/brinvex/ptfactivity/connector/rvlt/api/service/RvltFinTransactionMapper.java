package com.brinvex.ptfactivity.connector.rvlt.api.service;

import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;

import java.util.List;

public interface RvltFinTransactionMapper {

    List<FinTransaction> mapTransactions(List<Transaction> rvltTransactions);

}
