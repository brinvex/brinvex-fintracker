package com.brinvex.fintracker.connector.ibkr.impl;

import com.brinvex.fintracker.api.FinTracker;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrAccount;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrModule;
import com.brinvex.fintracker.test.support.TestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

abstract class BaseIbkrTest {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected final TestSupport testSupport = new TestSupport("connector-ibkr");

    protected final IbkrAccount account1 = IbkrAccount.of(testSupport.subProperties("account1"));

    protected final IbkrAccount account2 = IbkrAccount.of(testSupport.subProperties("account2"));

    public boolean account1IsNotNull() {
        return account1 != null;
    }

    public boolean account2IsNotNull() {
        return account2 != null;
    }

    public boolean account2MigratedIsNotNull() {
        return account2 != null && account2.migratedAccount() != null;
    }

    public boolean account2CredentialsIsNotNull() {
        return account2 != null && account2.credentials() != null;
    }

    protected IbkrModule createIbkrModule(String workspace) {
        return testSupport.finTracker(Map.of(IbkrModule.PROP_DMS_WORKSPACE, workspace)).get(IbkrModule.class);
    }

    protected FinTracker createFinTracker(String workspace) {
        return testSupport.finTracker(Map.of(IbkrModule.PROP_DMS_WORKSPACE, workspace));
    }

}
