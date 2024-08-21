package com.brinvex.fintracker.connector.ibkr.api.service;


import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;

import java.time.LocalDate;
import java.util.List;

public interface IbkrDms {

    List<ActivityDocKey> getActivityDocKeys(String accountId, LocalDate fromDayIncl, LocalDate toDayIncl);

    List<TradeConfirmDocKey> getTradeConfirmDocKeys(String accountId, LocalDate fromDayIncl, LocalDate toDayIncl);

    TradeConfirmDocKey getUniqueTradeConfirmDocKey(String accountId, LocalDate day);

    String getStatementContent(IbkrDocKey docKey);

    boolean putStatementIfUseful(ActivityDocKey docKey, String content);

    boolean putStatement(IbkrDocKey docKey, String content);

    void delete(IbkrDocKey docKey);

}
