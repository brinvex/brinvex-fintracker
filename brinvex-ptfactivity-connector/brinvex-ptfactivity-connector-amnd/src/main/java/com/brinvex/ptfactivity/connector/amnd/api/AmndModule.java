package com.brinvex.ptfactivity.connector.amnd.api;

import com.brinvex.ptfactivity.connector.amnd.api.service.AmndDms;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndFinTransactionMapper;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndPtfActivityProvider;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndStatementParser;
import com.brinvex.ptfactivity.core.api.Module;

public interface AmndModule extends Module {

    AmndStatementParser statementParser();

    AmndDms dms();

    AmndFinTransactionMapper finTransactionMapper();

    AmndPtfActivityProvider ptfProgressProvider();

}
