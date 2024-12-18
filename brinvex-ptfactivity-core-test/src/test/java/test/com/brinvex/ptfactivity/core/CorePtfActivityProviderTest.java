package test.com.brinvex.ptfactivity.core;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.PtfActivityRuntime;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction.FinTransactionConstraints;
import com.brinvex.ptfactivity.testsupport.SimplePtf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.math.BigDecimal;
import java.util.Set;

import static com.brinvex.finance.types.enu.Currency.EUR;
import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorePtfActivityProviderTest extends CoreBaseTest {

    // See also FiobPtfProgressOfflineTest.ptfProgress7_saving_ended
    @EnabledIf("ptf1IsNotNull")
    @Test
    void ptfProgress1() {
        PtfActivityReq ptfActivityReq = new PtfActivityReq(
                "core",
                ptf1,
                parse("2022-01-01"),
                parse("2024-11-01"),
                null
        );
        PtfActivity ptfActivity = testCtx.runtime().process(ptfActivityReq);
        assertNotNull(ptfActivity);

        testCtx.validator().validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

        SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
        assertEquals(Set.of(Currency.EUR), ptf.getCurrencies());

        assertEquals(0, ptf.getCash(Currency.EUR).compareTo(ZERO));
    }

    @EnabledIf("ptf2IsNotNull")
    @Test
    void ptfProgress2() {
        PtfActivityRuntime ptfActivityRuntime = testCtx.runtime();

        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    ptf2,
                    parse("2023-01-01"),
                    parse("2023-10-01"),
                    null
            );
            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            assertNotNull(ptfActivity);

            testCtx.validator().validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf.getCurrencies());

            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("1012.35")));
        }
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    ptf2,
                    parse("2023-01-01"),
                    parse("2023-10-02"),
                    null
            );
            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            assertNotNull(ptfActivity);

            testCtx.validator().validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf.getCurrencies());

            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));
        }
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    ptf2,
                    parse("2023-01-01"),
                    parse("2023-12-31"),
                    null
            );
            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            assertNotNull(ptfActivity);

            testCtx.validator().validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf.getCurrencies());

            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));
        }
    }

    @EnabledIf("ptf3IsNotNull")
    @Test
    void ptfProgress3() {
        PtfActivityRuntime ptfActivityRuntime = testCtx.runtime();

        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    ptf3,
                    parse("2018-10-01"),
                    parse("2023-04-27"),
                    null
            );
            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            assertNotNull(ptfActivity);

            testCtx.validator().validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf.getCurrencies());

            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));
            assertEquals(1, ptf.getHoldingsCount());
            assertEquals(6, ptf.getHoldingQty("SK", "BP23Y05C04").intValue());
        }
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    ptf3,
                    parse("2023-01-01"),
                    parse("2023-10-31"),
                    null
            );
            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            assertNotNull(ptfActivity);

            testCtx.validator().validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf.getCurrencies());

            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));
        }
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "core",
                    ptf2,
                    parse("2023-01-01"),
                    parse("2023-12-31"),
                    null
            );
            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            assertNotNull(ptfActivity);

            testCtx.validator().validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
            assertEquals(Set.of(EUR), ptf.getCurrencies());

            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));
            assertEquals(0, ptf.getHoldingsCount());
        }
    }
}
