package test.com.brinvex.ptfactivity.connector.fiob;


import com.brinvex.finance.types.vo.DateAmount;
import com.brinvex.ptfactivity.connector.fiob.api.FiobModule;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobPtfActivityProvider;
import com.brinvex.ptfactivity.core.api.PtfActivityRuntime;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction.FinTransactionConstraints;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;
import com.brinvex.ptfactivity.testsupport.ObfuscationUtil;
import com.brinvex.ptfactivity.testsupport.SimplePtf;
import com.brinvex.ptfactivity.testsupport.TestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;

import static com.brinvex.finance.types.enu.Currency.EUR;
import static com.brinvex.ptfactivity.testsupport.AssertionUtil.assertPtfSnapshotEqual;
import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiobPtfActivityOfflineTest extends FiobBaseTest {

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress1() {
        assert account1 != null;

        String dmsWorkspace = "fiob-dms-stable-20231231";
        TestContext testCtx = this.testCtx.withDmsWorkspace(dmsWorkspace);
        FiobModule fiobModule = testCtx.get(FiobModule.class);
        FiobPtfActivityProvider ptfProgressProvider = fiobModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        LocalDate fromDateIncl = parse("2019-01-01");
        LocalDate toDateIncl = parse("2023-12-31");

        SimplePtf expectedPtf = loadExpectedPtfSnapshot(testCtx, account1, toDateIncl);

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account1, fromDateIncl, toDateIncl);

        SequencedCollection<FinTransaction> trans = ptfActivity.transactions();
        assertFalse(trans.isEmpty());
        validator.validateAndThrow(trans, FinTransactionConstraints::of);

        SimplePtf ptf = new SimplePtf(trans);
        assertPtfSnapshotEqual(expectedPtf, ptf);
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress2_SK() {
        assert account1 != null;

        String dmsWorkspace = "fiob-dms-stable-20221231_SK";
        TestContext testCtx = this.testCtx.withDmsWorkspace(dmsWorkspace);
        FiobModule fiobModule = testCtx.get(FiobModule.class);
        FiobPtfActivityProvider ptfProgressProvider = fiobModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        LocalDate fromDateIncl = parse("2019-01-01");
        LocalDate toDateIncl = parse("2022-12-31");

        SimplePtf expectedPtf = loadExpectedPtfSnapshot(testCtx, account1, toDateIncl);

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account1, fromDateIncl, toDateIncl);

        SequencedCollection<FinTransaction> trans = ptfActivity.transactions();
        assertFalse(trans.isEmpty());
        validator.validateAndThrow(trans, FinTransactionConstraints::of);

        SimplePtf ptf = new SimplePtf(trans);
        assertPtfSnapshotEqual(expectedPtf, ptf);
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress3_CZ() {
        assert account1 != null;

        String dmsWorkspace = "fiob-dms-stable-20221231_CZ";
        TestContext testCtx = this.testCtx.withDmsWorkspace(dmsWorkspace);
        FiobModule fiobModule = testCtx.get(FiobModule.class);
        FiobPtfActivityProvider ptfProgressProvider = fiobModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        LocalDate fromDateIncl = parse("2019-01-01");
        LocalDate toDateIncl = parse("2022-12-31");

        SimplePtf expectedPtf = loadExpectedPtfSnapshot(testCtx, account1, toDateIncl);

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account1, fromDateIncl, toDateIncl);

        SequencedCollection<FinTransaction> trans = ptfActivity.transactions();
        assertFalse(trans.isEmpty());
        validator.validateAndThrow(trans, FinTransactionConstraints::of);

        SimplePtf ptf = new SimplePtf(trans);
        assertPtfSnapshotEqual(expectedPtf, ptf);
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress4_EN() {
        assert account1 != null;

        String dmsWorkspace = "fiob-dms-stable-20221231_EN";
        TestContext testCtx = this.testCtx.withDmsWorkspace(dmsWorkspace);
        FiobModule fiobModule = testCtx.get(FiobModule.class);
        FiobPtfActivityProvider ptfProgressProvider = fiobModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        LocalDate fromDateIncl = parse("2019-01-01");
        LocalDate toDateIncl = parse("2022-12-31");

        SimplePtf expectedPtf = loadExpectedPtfSnapshot(testCtx, account1, toDateIncl);

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account1, fromDateIncl, toDateIncl);

        SequencedCollection<FinTransaction> trans = ptfActivity.transactions();
        assertFalse(trans.isEmpty());
        validator.validateAndThrow(trans, FinTransactionConstraints::of);

        SimplePtf ptf = new SimplePtf(trans);
        assertPtfSnapshotEqual(expectedPtf, ptf);
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress5_id() {
        assert account1 != null;

        String dmsWorkspace = "fiob-dms-stable-20231231";
        TestContext testCtx = this.testCtx.withDmsWorkspace(dmsWorkspace);
        FiobModule fiobModule = testCtx.get(FiobModule.class);
        FiobPtfActivityProvider ptfProgressProvider = fiobModule.ptfProgressProvider();

        LocalDate fromDateIncl = parse("2019-01-01");
        LocalDate toDateIncl = parse("2023-12-31");

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account1, fromDateIncl, toDateIncl);

        SequencedCollection<FinTransaction> trans = ptfActivity.transactions();
        assertFalse(trans.isEmpty());

        Map<String, FinTransaction> idToTrans = new LinkedHashMap<>();
        for (FinTransaction tran : trans) {
            String extraId = tran.externalId();
            assertNotNull(extraId);
            FinTransaction duplicate = idToTrans.put(extraId, tran);
            assertNull(duplicate);
        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress5_finTranConstraints() {
        String dmsWorkspace = "fiob-dms-stable-20231231";
        TestContext testCtx = this.testCtx.withDmsWorkspace(dmsWorkspace);
        FiobModule fiobModule = testCtx.get(FiobModule.class);
        FiobPtfActivityProvider ptfProgressProvider = fiobModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        LocalDate fromDateIncl = parse("2019-01-01");
        LocalDate toDateIncl = parse("2023-12-31");

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account1, fromDateIncl, toDateIncl);

        for (FinTransaction t : ptfActivity.transactions()) {
            validator.validate(FinTransactionConstraints.of(t));

            FinTransactionType type = t.type();
            Asset asset = t.asset();
            if (type.equals(FinTransactionType.FEE)) {
                if (t.externalDetail().contains("ADR")) {
                    assertNotNull(asset.symbol());
                    assertNotNull(asset.country());
                }
            } else if (type.equals(FinTransactionType.DIVIDEND)) {
                if (t.externalDetail().contains(" daň ")) {
                    assertTrue(t.tax().compareTo(ZERO) < 0);
                }
            }

        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress6() {
        assert account1 != null;

        String dmsWorkspace = "fiob-dms-stable-20231231";
        TestContext testCtx = this.testCtx.withDmsWorkspace(dmsWorkspace);
        FiobModule fiobModule = testCtx.get(FiobModule.class);
        FiobPtfActivityProvider ptfProgressProvider = fiobModule.ptfProgressProvider();

        LocalDate fromDateIncl = parse("2019-01-01");
        LocalDate toDateIncl = parse("2023-12-31");

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account1, fromDateIncl, toDateIncl);
        SequencedCollection<DateAmount> navs = ptfActivity.netAssetValues();
        DateAmount newestNav = navs.getLast();
        assertEquals(new DateAmount("2023-12-31", "53.49"), newestNav.with(ObfuscationUtil.remainder100));
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void ptfProgress7_saving_ended() {
        assert account2 != null;

        TestContext testCtx = this.testCtx.withDmsWorkspace("fiob-core-dms-stable-20241101");
        ValidatorFacade validator = testCtx.validator();
        PtfActivityRuntime ptfActivityRuntime = testCtx.runtime();

        {
            LocalDate fromDateIncl = parse("2022-01-01");
            LocalDate toDateIncl = parse("2024-11-01");

            PtfActivityReq ptfActivityReq1 = new PtfActivityReq(
                    "fiob",
                    account2,
                    fromDateIncl,
                    toDateIncl,
                    null
            );

            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq1);
            assertFalse(ptfActivity.transactions().isEmpty());
            assertNull(ptfActivity.netAssetValues());

            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf1 = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf1.getCurrencies());

            assertEquals(0, ptf1.getCash(EUR).compareTo(ZERO));

            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    account2,
                    fromDateIncl,
                    toDateIncl,
                    null
            );
            PtfActivity ptfActivity2 = ptfActivityRuntime.process(ptfActivityReq);
            assertFalse(ptfActivity2.transactions().isEmpty());
            assertNull(ptfActivity2.netAssetValues());
            validator.validateAndThrow(ptfActivity2.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf2 = new SimplePtf(ptfActivity2.transactions());

            assertPtfSnapshotEqual(ptf1, ptf2);
        }
        {
            LocalDate fromDateIncl = parse("2022-01-01");
            LocalDate toDateIncl = parse("2024-06-30");

            PtfActivityReq ptfActivityReq1 = new PtfActivityReq(
                    "fiob",
                    account2,
                    fromDateIncl,
                    toDateIncl,
                    null
            );

            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq1);
            assertFalse(ptfActivity.transactions().isEmpty());
            assertNull(ptfActivity.netAssetValues());

            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf1 = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf1.getCurrencies());

            assertEquals(0, ptf1.getCash(EUR).compareTo(new BigDecimal("1724.66")));

            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    account2,
                    fromDateIncl,
                    toDateIncl,
                    null
            );
            PtfActivity ptfActivity2 = ptfActivityRuntime.process(ptfActivityReq);
            assertFalse(ptfActivity2.transactions().isEmpty());
            assertNull(ptfActivity2.netAssetValues());
            validator.validateAndThrow(ptfActivity2.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf2 = new SimplePtf(ptfActivity2.transactions());

            assertPtfSnapshotEqual(ptf1, ptf2);
        }
    }

}

