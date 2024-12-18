package com.brinvex.ptfactivity.connector.rvlt.api.service;

import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.PnlStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TradingAccountStatement;

public interface RvltStatementParser {

    TradingAccountStatement parseTradingAccountStatement(byte[] statementPdfContent);

    PnlStatement parsePnlStatement(byte[] statementPdfContent);
}
