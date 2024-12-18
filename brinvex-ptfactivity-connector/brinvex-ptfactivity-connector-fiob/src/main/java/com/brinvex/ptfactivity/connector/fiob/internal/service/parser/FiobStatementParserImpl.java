package com.brinvex.ptfactivity.connector.fiob.internal.service.parser;

import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobStatementParser;

import java.time.LocalDateTime;
import java.util.List;

public class FiobStatementParserImpl implements FiobStatementParser {

    private final FiobTradingStatementParser tradingTransStatementParser;

    private final FiobSavingStatementParser savingStatementParser;

    public FiobStatementParserImpl() {
        tradingTransStatementParser = new FiobTradingStatementParser();
        savingStatementParser = new FiobSavingStatementParser();
    }

    @Override
    public LocalDateTime parseTradingStatementCreatedOn(List<String> statementHeaderLines) {
        return tradingTransStatementParser.parseTradingTransStatementCreatedOn(statementHeaderLines);
    }

    @Override
    public Statement.TradingTransStatement parseTradingTransStatement(String statementContent) {
        return tradingTransStatementParser.parseTransactionStatement(statementContent);
    }

    @Override
    public Statement.SavingTransStatement parseSavingTransStatement(String statementContent) {
        return savingStatementParser.parseStatement(statementContent);
    }

    @Override
    public Statement.TradingSnapshotStatement parseSnapshotStatement(String statementContent) {
        return tradingTransStatementParser.parsePtfSnapshotStatement(statementContent);
    }
}
