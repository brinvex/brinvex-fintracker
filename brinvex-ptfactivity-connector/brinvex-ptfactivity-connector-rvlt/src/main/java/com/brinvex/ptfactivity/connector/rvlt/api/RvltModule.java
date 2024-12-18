package com.brinvex.ptfactivity.connector.rvlt.api;

import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMapper;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltDms;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMerger;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltPtfActivityProvider;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltStatementParser;
import com.brinvex.ptfactivity.core.api.Module;

public interface RvltModule extends Module {

    RvltStatementParser statementParser();

    RvltDms dms();

    RvltFinTransactionMerger finTransactionMerger();

    RvltFinTransactionMapper finTransactionMapper();

    RvltPtfActivityProvider ptfProgressProvider();

}
