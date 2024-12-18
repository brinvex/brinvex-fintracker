package test.com.brinvex.ptfactivity.core;

import com.brinvex.ptfactivity.core.api.CoreModule;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.testsupport.TestContext;

abstract class CoreBaseTest {

    protected final TestContext testCtx = new TestContext(CoreModule.class);

    protected final Account ptf1 = Account.of(testCtx.subProperties("ptf1"));

    protected final Account ptf2 = Account.of(testCtx.subProperties("ptf2"));

    protected final Account ptf3 = Account.of(testCtx.subProperties("ptf3"));

    public boolean ptf1IsNotNull() {
        return ptf1 != null;
    }

    public boolean ptf2IsNotNull() {
        return ptf2 != null;
    }

    public boolean ptf3IsNotNull() {
        return ptf3 != null;
    }
}
