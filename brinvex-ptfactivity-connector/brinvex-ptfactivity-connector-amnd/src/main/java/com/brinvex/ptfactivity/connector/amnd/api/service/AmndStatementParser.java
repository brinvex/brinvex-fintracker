package com.brinvex.ptfactivity.connector.amnd.api.service;

import com.brinvex.ptfactivity.connector.amnd.api.model.statement.TransactionStatement;

public interface AmndStatementParser {

    TransactionStatement parseTrades(byte[] statementPdfContent);
}
