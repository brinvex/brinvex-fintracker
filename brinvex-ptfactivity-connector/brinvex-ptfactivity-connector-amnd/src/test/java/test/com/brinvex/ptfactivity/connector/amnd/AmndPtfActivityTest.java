package test.com.brinvex.ptfactivity.connector.amnd;

import com.brinvex.ptfactivity.core.api.PtfActivityRuntime;
import com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction.FinTransactionConstraints;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.testsupport.SimplePtf;
import com.brinvex.java.Num;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;

import static com.brinvex.finance.types.enu.Currency.EUR;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmndPtfActivityTest extends AmndBaseTest {

    private final PtfActivityRuntime ptfActivityRuntime = testCtx.runtime();

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress1() {
        assert account1 != null;
        BigDecimal feeSum1;
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "amnd",
                    account1.withExtraProp("initialFeeReserve", null),
                    parse("2017-01-01"),
                    parse("2024-11-26"),
                    null
            );

            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            List<FinTransaction> trans = new ArrayList<>(ptfActivity.transactions());
            testCtx.validator().validateAndThrow(trans, FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
            assertEquals(FinTransactionType.DEPOSIT, trans.get(0).type());
            assertEquals(0, trans.get(0).fee().compareTo(ZERO));
            assertEquals(FinTransactionType.BUY, trans.get(1).type());
            assertEquals(0, trans.get(1).fee().remainder(Num._100).compareTo(new BigDecimal("-70.00")));

            assertEquals(1, ptf.getHoldingsCount());
            assertEquals(0, ptf.getHoldingQty("DE", "0P00015DFW").remainder(TEN)
                    .compareTo((new BigDecimal("3.945"))));
            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));

            feeSum1 = trans.stream().map(FinTransaction::fee).reduce(ZERO, BigDecimal::add);
        }
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "amnd",
                    account1,
                    parse("2017-01-01"),
                    parse("2024-11-26"),
                    null
            );

            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            List<FinTransaction> trans = new ArrayList<>(ptfActivity.transactions());
            testCtx.validator().validateAndThrow(trans, FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(FinTransactionType.DEPOSIT, trans.get(0).type());
            assertEquals(0, trans.get(0).fee().compareTo(ZERO));
            assertEquals(FinTransactionType.BUY, trans.get(1).type());
            assertEquals(0, trans.get(1).fee().compareTo(new BigDecimal("-3.001795940000")));

            assertEquals(FinTransactionType.DEPOSIT, trans.get(2).type());
            assertEquals(0, trans.get(2).fee().compareTo(ZERO));
            assertEquals(FinTransactionType.BUY, trans.get(3).type());
            assertEquals(0, trans.get(3).fee().compareTo(new BigDecimal("-4.618147600000")));

            BigDecimal feeSum2 = trans.stream().map(FinTransaction::fee).reduce(ZERO, BigDecimal::add).setScale(4, HALF_UP);
            assertEquals(0, feeSum1.compareTo(feeSum2));

            assertEquals(1, ptf.getHoldingsCount());
            assertEquals(0, ptf.getHoldingQty("DE", "0P00015DFW").remainder(TEN)
                    .compareTo((new BigDecimal("3.945"))));
            assertEquals(0, ptf.getCash(EUR).setScale(4, HALF_UP).compareTo(ZERO));
        }
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void ptfProgress2() {
        assert account2 != null;
        BigDecimal feeSum1;
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "amnd",
                    account2.withExtraProp("initialFeeReserve", null),
                    parse("2017-01-01"),
                    parse("2024-11-26"),
                    null
            );

            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            SequencedCollection<FinTransaction> trans = ptfActivity.transactions();
            testCtx.validator().validateAndThrow(trans, FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(trans);
            assertEquals(1, ptf.getHoldingsCount());
            assertEquals(0, ptf.getHoldingQty("DE", "0P00015DFN").remainder(TEN)
                    .compareTo((new BigDecimal("0.472"))));
            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));

            feeSum1 = trans.stream().map(FinTransaction::fee).reduce(ZERO, BigDecimal::add);
        }
        {
            {
                PtfActivityReq ptfActivityReq = new PtfActivityReq(
                        "amnd",
                        account2,
                        parse("2017-01-01"),
                        parse("2024-11-26"),
                        null
                );

                PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
                List<FinTransaction> trans = new ArrayList<>(ptfActivity.transactions());
                testCtx.validator().validateAndThrow(trans, FinTransactionConstraints::of);

                SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

                BigDecimal feeSum2 = trans.stream().map(FinTransaction::fee).reduce(ZERO, BigDecimal::add).setScale(4, HALF_UP);
                assertEquals(0, feeSum1.compareTo(feeSum2));

                assertEquals(1, ptf.getHoldingsCount());
                assertEquals(0, ptf.getHoldingQty("DE", "0P00015DFN").remainder(TEN)
                        .compareTo((new BigDecimal("0.472"))));
                assertEquals(0, ptf.getCash(EUR).setScale(4, HALF_UP).compareTo(ZERO));
            }
        }
    }

    @EnabledIf("account3IsNotNull")
    @Test
    void ptfProgress3() {
        assert account3 != null;
        BigDecimal feeSum1;
        {
            PtfActivityReq ptfActivityReq = new PtfActivityReq(
                    "amnd",
                    account3.withExtraProp("initialFeeReserve", null),
                    parse("2017-01-01"),
                    parse("2024-11-26"),
                    null
            );

            PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
            SequencedCollection<FinTransaction> trans = ptfActivity.transactions();
            testCtx.validator().validateAndThrow(trans, FinTransactionConstraints::of);

            SimplePtf ptf = new SimplePtf(trans);
            assertEquals(1, ptf.getHoldingsCount());
            assertEquals(0, ptf.getHoldingQty("DE", "0P00015DFR").remainder(TEN)
                    .compareTo((new BigDecimal("5.889"))));
            assertEquals(0, ptf.getCash(EUR).compareTo(ZERO));

            feeSum1 = trans.stream().map(FinTransaction::fee).reduce(ZERO, BigDecimal::add);
        }
        {
            {
                PtfActivityReq ptfActivityReq = new PtfActivityReq(
                        "amnd",
                        account3,
                        parse("2017-01-01"),
                        parse("2024-11-26"),
                        null
                );

                PtfActivity ptfActivity = ptfActivityRuntime.process(ptfActivityReq);
                List<FinTransaction> trans = new ArrayList<>(ptfActivity.transactions());
                testCtx.validator().validateAndThrow(trans, FinTransactionConstraints::of);

                SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

                BigDecimal feeSum2 = trans.stream().map(FinTransaction::fee).reduce(ZERO, BigDecimal::add).setScale(4, HALF_UP);
                assertEquals(0, feeSum1.compareTo(feeSum2));

                assertEquals(1, ptf.getHoldingsCount());
                assertEquals(0, ptf.getHoldingQty("DE", "0P00015DFR").remainder(TEN)
                        .compareTo((new BigDecimal("5.889"))));
                assertEquals(0, ptf.getCash(EUR).setScale(4, HALF_UP).compareTo(ZERO));
            }
        }
    }
}
