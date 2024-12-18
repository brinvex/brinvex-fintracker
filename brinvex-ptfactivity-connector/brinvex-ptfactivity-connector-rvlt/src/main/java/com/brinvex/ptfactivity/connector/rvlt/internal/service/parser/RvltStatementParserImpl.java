package com.brinvex.ptfactivity.connector.rvlt.internal.service.parser;

import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.PnlStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TradingAccountStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltStatementParser;
import com.brinvex.ptfactivity.core.api.facade.PdfReaderFacade;

import java.util.List;

public class RvltStatementParserImpl implements RvltStatementParser {

    private final PdfReaderFacade pdfReader;

    private final RvltTradingAccStatementParser tradingAccStatementParser;

    private final RvltPnlStatementParser pnlStatementParser;

    public RvltStatementParserImpl(PdfReaderFacade pdfReader) {
        this.pdfReader = pdfReader;
        this.tradingAccStatementParser = new RvltTradingAccStatementParser();
        this.pnlStatementParser = new RvltPnlStatementParser();
    }

    @Override
    public TradingAccountStatement parseTradingAccountStatement(byte[] statementPdfContent) {
        List<String> lines = pdfReader.readPdfLines(statementPdfContent);
        String accountStatementTitle = "Account Statement";
        String line0 = lines.get(0);
        String line1 = lines.get(1);
        if (!accountStatementTitle.equals(line0) && !accountStatementTitle.equals(line1)) {
            throw new IllegalArgumentException("Account Statement header line not found, given: %s, %s".formatted(line0, line1));
        }
        return tradingAccStatementParser.parseAccountStatement(lines);
    }

    @Override
    public PnlStatement parsePnlStatement(byte[] statementPdfContent) {
        List<String> lines = pdfReader.readPdfLines(statementPdfContent);
        String eurPnlStatementTitle = "EUR Profit and Loss Statement";
        String usdPnlStatementTitle = "USD Profit and Loss Statement";
        String line1 = lines.get(1);
        if (!eurPnlStatementTitle.equals(line1) && !usdPnlStatementTitle.equals(line1)) {
            throw new IllegalArgumentException("Profit and Loss Statement header line not found, given: %s".formatted(line1));
        }
        return pnlStatementParser.parseAccountStatement(lines);
    }
}
