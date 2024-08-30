package com.brinvex.fintracker.connector.ibkr.api.service;


import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IbkrDms {

    List<ActivityDocKey> getActivityDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl);

    List<TradeConfirmDocKey> getTradeConfirmDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl);

    TradeConfirmDocKey getUniqueTradeConfirmDocKey(String accountId, LocalDate date);

    String getStatementContent(IbkrDocKey docKey);

    boolean putStatementIfUseful(ActivityDocKey docKey, String content);

    boolean putStatement(IbkrDocKey docKey, String content);

    void delete(IbkrDocKey docKey);

    void putMetaProperties(String accountId, String metaFileName, Map<String, String> metaProperties);

    Map<String, String> getMetaProperties(String accountId, String metaFileName);
}
