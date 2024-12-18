package test.com.brinvex.ptfactivity.connector.ibkr;

import com.brinvex.ptfactivity.connector.ibkr.api.IbkrModule;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrAccount;
import com.brinvex.ptfactivity.testsupport.TestContext;

abstract class IbkrBaseTest {

    protected final TestContext testCtx = new TestContext(IbkrModule.class);

    protected final IbkrAccount account1 = IbkrAccount.of(testCtx.subProperties("account1"));

    protected final IbkrAccount account2 = IbkrAccount.of(testCtx.subProperties("account2"));

    public boolean account1IsNotNull() {
        return account1 != null;
    }

    public boolean account2IsNotNull() {
        return account2 != null;
    }

    public boolean account2IsMigrated() {
        return account2 != null && !account2.migratedAccounts().isEmpty();
    }

    public boolean account2CredentialsIsNotNull() {
        return account2 != null && account2.credentials() != null;
    }

}
