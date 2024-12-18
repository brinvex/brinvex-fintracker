package com.brinvex.ptfactivity.connector.amnd.api.service;

import com.brinvex.ptfactivity.connector.amnd.api.model.AmndTransStatementDocKey;

public interface AmndDms {

    AmndTransStatementDocKey getTradingAccountStatementDocKey(String accountNumber);

    byte[] getStatementContent(AmndTransStatementDocKey docKey);

}
