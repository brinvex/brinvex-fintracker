package test.com.brinvex.ptfactivity.connector.rvlt.impl.service;

import com.brinvex.ptfactivity.connector.rvlt.api.RvltModule;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.testsupport.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class RvltBaseTest {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected final TestContext testCtx = new TestContext(RvltModule.class);

    protected final Account account1 = Account.of(testCtx.subProperties("account1"));

    public boolean account1IsNotNull() {
        return account1 != null;
    }
}
