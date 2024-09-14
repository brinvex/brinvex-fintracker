package com.brinvex.fintracker.connector.ibkr.impl;

import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrAccount;
import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.test.support.ModuleTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

abstract class BaseIbkrTest {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected final ModuleTestSupport moduleTestSupport = new ModuleTestSupport(IbkrModule.class);

    protected final IbkrAccount account1 = IbkrAccount.of(moduleTestSupport.subProperties("account1"));

    protected final IbkrAccount account2 = IbkrAccount.of(moduleTestSupport.subProperties("account2"));

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

    protected IbkrModule newIbkrModule(String dmsWorkspace) {
        return newFinTracker(dmsWorkspace).module(IbkrModule.class);
    }

    protected FinTracker newFinTracker(String dmsWorkspace) {
        return moduleTestSupport.finTracker(Map.of(FinTrackerModule.PropKey.dmsWorkspace, dmsWorkspace));
    }

}
