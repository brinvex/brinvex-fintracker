package com.brinvex.fintracker.connector.ibkr.api.service;

import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;

public interface IbkrStatementParser {

    ActivityStatement parseActivityStatement(String statementXmlContent);

    TradeConfirmStatement parseTradeConfirmStatement(String statementXmlContent);

}
