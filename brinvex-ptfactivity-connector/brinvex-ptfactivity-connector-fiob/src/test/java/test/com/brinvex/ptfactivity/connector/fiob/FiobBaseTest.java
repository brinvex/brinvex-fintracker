package test.com.brinvex.ptfactivity.connector.fiob;

import com.brinvex.ptfactivity.connector.fiob.api.FiobModule;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.facade.JsonMapperFacade;
import com.brinvex.ptfactivity.testsupport.SimplePtf;
import com.brinvex.ptfactivity.testsupport.TestContext;
import com.brinvex.dms.api.Dms;

import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

public abstract class FiobBaseTest {

    protected final TestContext testCtx = new TestContext(FiobModule.class);

    protected final Account account1 = Account.of(testCtx.subProperties("account1"));

    protected final Account account2 = Account.of(testCtx.subProperties("account2"));

    protected final Account account3 = Account.of(testCtx.subProperties("account3"));

    protected final Account account4 = Account.of(testCtx.subProperties("account4"));

    public boolean account1IsNotNull() {
        return account1 != null;
    }

    public boolean account2IsNotNull() {
        return account2 != null;
    }

    public boolean account1CredentialsIsNotNull() {
        return account1 != null && account1.credentials() != null;
    }

    public boolean account2CredentialsIsNotNull() {
        return account2 != null && account2.credentials() != null;
    }

    public boolean account3IsNotNull() {
        return account3 != null;
    }

    public boolean account3CredentialsIsNotNull() {
        return account3 != null && account3.credentials() != null;
    }

    public boolean account4IsNotNull() {
        return account4 != null;
    }

    public boolean account4CredentialsIsNotNull() {
        return account4 != null && account4.credentials() != null;
    }

    protected SimplePtf loadExpectedPtfSnapshot(TestContext testCtx, Account account, LocalDate date) {
        Dms dms = testCtx.dmsFactory().getDms(testCtx.dmsWorkspace());
        JsonMapperFacade jsonMapper = testCtx.toolbox().jsonMapper();
        return jsonMapper.readFromJson(
                dms.getTextContent(account.externalId(), "%s-Snapshot-%s-expected.json".formatted(
                        account.externalId(),
                        date.format(BASIC_ISO_DATE)
                )),
                SimplePtf.class);
    }

}
