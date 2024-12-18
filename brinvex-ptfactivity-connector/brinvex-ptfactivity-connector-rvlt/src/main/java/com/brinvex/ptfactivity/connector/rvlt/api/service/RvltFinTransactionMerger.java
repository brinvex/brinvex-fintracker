package com.brinvex.ptfactivity.connector.rvlt.api.service;

import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;

import java.util.List;

public interface RvltFinTransactionMerger {

    List<Transaction> mergeTransactions(List<Transaction> taTrans, List<Transaction> pnlTransactions);

}
