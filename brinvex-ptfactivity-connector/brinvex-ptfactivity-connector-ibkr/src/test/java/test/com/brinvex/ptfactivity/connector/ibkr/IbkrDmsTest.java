package test.com.brinvex.ptfactivity.connector.ibkr;


import com.brinvex.ptfactivity.connector.ibkr.api.IbkrModule;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrDms;
import com.brinvex.ptfactivity.testsupport.TestContext;
import com.brinvex.dms.api.Dms;
import com.brinvex.dms.api.DmsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbkrDmsTest extends IbkrBaseTest {

    @EnabledIf("account1IsNotNull")
    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    @Test
    void dmsConsistency() {
        assert account1 != null;

        String dmsWorkspace = "ibkr-dms-tmp1";
        TestContext ptfactivity = testCtx.withDmsWorkspace(dmsWorkspace);
        DmsFactory dmsFactory = ptfactivity.dmsFactory();
        Dms dms = dmsFactory.getDms(dmsWorkspace);
        dms.resetWorkspace();
        dms.purgeWorkspace(LocalDateTime.now());
        IbkrDms ibkrDms = ptfactivity.get(IbkrModule.class).dms();

        List<ActivityDocKey> actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertTrue(actDocKeys.isEmpty());

        boolean saved;
        String content1 = "test content 1 " + UUID.randomUUID();
        String content2 = "test content 2 " + UUID.randomUUID();
        String content3 = "test content 3 " + UUID.randomUUID();
        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-04"), parse("2024-08-06")), content1);
        assertTrue(saved);
        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-04"), parse("2024-08-06")), content2);
        assertFalse(saved);

        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(1, actDocKeys.size());
        assertEquals(content1, ibkrDms.getStatementContent(actDocKeys.get(0)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-04"), parse("2024-08-07")), content2);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(1, actDocKeys.size());
        assertEquals(content2, ibkrDms.getStatementContent(actDocKeys.get(0)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-17"), parse("2024-08-20")), content3);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(2, actDocKeys.size());
        assertEquals(content2, ibkrDms.getStatementContent(actDocKeys.get(0)));
        assertEquals(content3, ibkrDms.getStatementContent(actDocKeys.get(1)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-04"), parse("2024-08-20")), content3);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(1, actDocKeys.size());
        assertEquals(content3, ibkrDms.getStatementContent(actDocKeys.get(0)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-24"), parse("2024-08-24")), content1);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(2, actDocKeys.size());
        assertEquals(content3, ibkrDms.getStatementContent(actDocKeys.get(0)));
        assertEquals(content1, ibkrDms.getStatementContent(actDocKeys.get(1)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-21"), parse("2024-08-24")), content2);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(2, actDocKeys.size());
        assertEquals(content3, ibkrDms.getStatementContent(actDocKeys.get(0)));
        assertEquals(content2, ibkrDms.getStatementContent(actDocKeys.get(1)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-03"), parse("2024-08-24")), content1);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(1, actDocKeys.size());
        assertEquals(content1, ibkrDms.getStatementContent(actDocKeys.get(0)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-25"), parse("2024-08-25")), content2);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(2, actDocKeys.size());
        assertEquals(content1, ibkrDms.getStatementContent(actDocKeys.get(0)));
        assertEquals(content2, ibkrDms.getStatementContent(actDocKeys.get(1)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-27"), parse("2024-08-27")), content3);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(3, actDocKeys.size());
        assertEquals(content1, ibkrDms.getStatementContent(actDocKeys.get(0)));
        assertEquals(content2, ibkrDms.getStatementContent(actDocKeys.get(1)));
        assertEquals(content3, ibkrDms.getStatementContent(actDocKeys.get(2)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-03"), parse("2024-08-25")), content1);
        assertFalse(saved);
        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-02"), parse("2024-08-25")), content1);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(2, actDocKeys.size());
        assertEquals(content1, ibkrDms.getStatementContent(actDocKeys.get(0)));
        assertEquals(content3, ibkrDms.getStatementContent(actDocKeys.get(1)));

        saved = ibkrDms.putActivityStatement(new ActivityDocKey(account1.externalId(), parse("2024-08-26"), parse("2024-08-26")), content2);
        assertTrue(saved);
        actDocKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, LocalDate.MAX);
        assertEquals(3, actDocKeys.size());
        assertEquals(content1, ibkrDms.getStatementContent(actDocKeys.get(0)));
        assertEquals(content2, ibkrDms.getStatementContent(actDocKeys.get(1)));
        assertEquals(content3, ibkrDms.getStatementContent(actDocKeys.get(2)));
    }
}

