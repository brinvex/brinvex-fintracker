package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.facade.ValidatorFacade;
import com.brinvex.fintracker.core.api.model.domain.FinTransaction;
import com.brinvex.fintracker.core.api.model.domain.FinTransactionType;
import com.brinvex.fintracker.core.api.model.domain.PtfProgress;
import com.brinvex.fintracker.core.api.model.domain.constraints.fintransaction.FinTransactionConstraints;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.test.support.SimplePtf;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.brinvex.fintracker.test.support.Country.US;
import static com.brinvex.fintracker.test.support.Currency.EUR;
import static com.brinvex.fintracker.test.support.Currency.USD;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class IbkrPtfProgressOfflineTest extends BaseIbkrTest {

    @EnabledIf("account2IsNotNull")
    @Test
    void portfolioProgress_iterative() {
        String workspace = "dms-stable";
        FinTracker finTracker = createFinTracker(workspace);
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        ValidatorFacade validator = finTracker.validator();
        IbkrDms ibkrDms = ibkrModule.dms();
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        LocalDate today = now();

        List<ActivityDocKey> docKeys = ibkrDms.getActivityDocKeys(account1.accountId(), LocalDate.MIN, today);
        assertFalse(docKeys.isEmpty());
        for (LocalDate d = docKeys.getFirst().fromDateIncl(); d.isBefore(docKeys.getLast().toDateIncl()); d = d.plusMonths(1)) {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(account2, d, today);
            assertNotNull(ptfProgress, "d=%s".formatted(d));

            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
        }
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void portfolioProgress_spinOff() {
        String workspace = "dms-stable";
        FinTracker finTracker = createFinTracker(workspace);
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = finTracker.validator();

        PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                account2, parse("2023-01-23"), parse("2024-04-02"));
        for (FinTransaction finTran : ptfProgress.transactions()) {
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
        }
        SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

        assertEquals(0, ptf.getHoldingQty(US, "GE").compareTo(new BigDecimal(6)));
        assertEquals(0, ptf.getHoldingQty(US, "GEV").compareTo(new BigDecimal(1)));
        assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("234.561374405")));
        assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("153.48807417")));

        FinTransaction transformationTran = ptf.getTransactions().get(225);
        assertEquals(FinTransactionType.TRANSFORMATION, transformationTran.type());
        assertEquals("GEV", transformationTran.asset().symbol());
        FinTransaction sellTran = ptf.getTransactions().get(226);
        assertEquals(FinTransactionType.SELL, sellTran.type());
        assertEquals("GEV", sellTran.asset().symbol());
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void portfolioProgress_paymentOfLieuOfDividends() {
        String workspace = "dms-stable";
        FinTracker finTracker = createFinTracker(workspace);
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = finTracker.validator();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-06-28"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(314, ptf.getTransactions().size());
            FinTransaction tran = ptf.getTransactions().get(310);
            assertEquals(tran.type(), FinTransactionType.CASH_DIVIDEND);
            assertEquals(tran.extraType(), "PAYMENT_IN_LIEU_OF_DIVIDENDS");
            assertEquals(tran.asset().symbol(), "ARCC");
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-07-15"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(326, ptf.getTransactions().size());
            FinTransaction tran = ptf.getTransactions().get(310);
            assertEquals(tran.type(), FinTransactionType.CASH_DIVIDEND);
            assertEquals(tran.extraType(), "PAYMENT_IN_LIEU_OF_DIVIDENDS");
            assertEquals(tran.asset().symbol(), "ARCC");
        }
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void portfolioProgress_tradeConfirm() {

        List<FinTransaction> actAndTcTrans;

        {
            FinTracker finTracker = createFinTracker("dms-stable-20240418");
            IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
            IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
            ValidatorFacade validator = finTracker.validator();

            List<FinTransaction> actTrans1;
            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-16"));
                for (FinTransaction finTran : ptfProgress.transactions()) {
                    Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                    assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
                }
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());
                actTrans1 = ptf.getTransactions();
                assertEquals(243, actTrans1.size());
                assertEquals(0, ptf.getHoldingQty(US, "MU").compareTo(new BigDecimal("1")));
            }
            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-17"));
                for (FinTransaction finTran : ptfProgress.transactions()) {
                    Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                    assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
                }
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());
                assertEquals(actTrans1, ptf.getTransactions());
            }
            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-18"));
                for (FinTransaction finTran : ptfProgress.transactions()) {
                    Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                    assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
                }
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

                actAndTcTrans = ptf.getTransactions();
                assertEquals(actTrans1.size() + 2, actAndTcTrans.size());

                FinTransaction newestTran = actAndTcTrans.getLast();
                assertEquals(FinTransactionType.BUY, newestTran.type());
                assertEquals(parse("2024-04-18"), newestTran.date());
                assertEquals("MU", newestTran.asset().symbol());
                assertEquals(0, ptf.getHoldingQty(US, "MU").compareTo(new BigDecimal("6")));
            }
        }
        {
            FinTracker finTracker = createFinTracker("dms-stable");
            IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
            IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
            ValidatorFacade validator = finTracker.validator();

            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-18"));
                for (FinTransaction finTran : ptfProgress.transactions()) {
                    Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                    assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
                }
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

                List<FinTransaction> actTrans2 = ptf.getTransactions();
                assertTrue(actAndTcTrans.size() <= actTrans2.size());

                Set<FinTransactionType> tcTranTypes = Set.of(FinTransactionType.BUY, FinTransactionType.SELL, FinTransactionType.FX_BUY, FinTransactionType.FX_SELL);
                for (int i = 0, j = 0; j < actTrans2.size(); i++, j++) {
                    FinTransaction actAndTcTran = actAndTcTrans.get(i);
                    FinTransaction actTran2 = actTrans2.get(i);
                    if (actAndTcTran.equals(actTran2)) {
                        continue;
                    }
                    if (actTran2.date().isEqual(parse("2024-04-18"))) {
                        if (!tcTranTypes.contains(actTran2.type())) {
                            i--;
                            continue;
                        }
                    }
                    fail("actAndTcTran=%s, actTran2=%s".formatted(actAndTcTran, actTran2));
                }
            }
        }
    }

    @EnabledIf("account2MigratedIsNotNull")
    @Test
    void ptfProgress_accountMigration_oldNavZero() {
        FinTracker finTracker = createFinTracker("dms-stable-20240904");
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = finTracker.validator();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2.migratedAccount().oldAccount(), parse("2023-01-23"), parse("2024-09-02"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals("0.00", ptf.getCash(EUR).setScale(2, HALF_UP).toString());
            assertEquals("0.00", ptf.getCash(USD).setScale(2, HALF_UP).toString());

            assertEquals(39, ptf.getHoldingsCount());
        }
    }

    @EnabledIf("account2MigratedIsNotNull")
    @Test
    void ptfProgress_accountMigration_newNavSameAsOld() {
        FinTracker finTracker = createFinTracker("dms-stable");
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = finTracker.validator();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-09-02"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            for (DateAmount nav : ptfProgress.netAssetValues()) {
                assertTrue(nav.amount().compareTo(ZERO) > 0, nav::toString);
            }

            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals("49.10", ptf.getCash(EUR).setScale(2, HALF_UP).toString());
            assertEquals("105.82", ptf.getCash(USD).setScale(2, HALF_UP).toString());

            assertEquals(39, ptf.getHoldingsCount());
        }
    }

    @EnabledIf("account2MigratedIsNotNull")
    @Test
    void ptfProgress_accountMigration_dividendAccruals() {
        FinTracker finTracker = createFinTracker("dms-stable-20240904");
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = finTracker.validator();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-09-04"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals("49.10", ptf.getCash(EUR).setScale(2, HALF_UP).toString());
            assertEquals("106.36", ptf.getCash(USD).setScale(2, HALF_UP).toString());

            int tranSize = ptf.getTransactions().size();

            FinTransaction tran_2 = ptf.getTransactions().get(tranSize - 3);
            assertEquals("2024-09-03", tran_2.date().toString());
            assertEquals(FinTransactionType.CASH_DIVIDEND, tran_2.type());
            assertEquals("0.54", tran_2.netValue().toString());
            assertEquals("0.63", tran_2.grossValue().toString());
            assertEquals("-0.09", tran_2.tax().toString());

            FinTransaction tran_1 = ptf.getTransactions().get(tranSize - 2);
            assertEquals("2024-09-04", tran_1.date().toString());
            assertEquals(FinTransactionType.WITHDRAWAL, tran_1.type());
            assertEquals("-0.54", tran_1.netValue().toString());

            FinTransaction tran_0 = ptf.getTransactions().get(tranSize - 1);
            assertEquals("2024-09-04", tran_0.date().toString());
            assertEquals(FinTransactionType.DEPOSIT, tran_0.type());
            assertEquals("0.54", tran_0.netValue().toString());

            assertEquals(39, ptf.getHoldingsCount());
        }
    }

    @EnabledIf("account2MigratedIsNotNull")
    @Test
    void ptfProgress_accountMigration_nav() {
        FinTracker finTracker = createFinTracker("dms-stable");
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = finTracker.validator();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-09-11"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());
            assertEquals(parse("2024-09-11"), ptfProgress.netAssetValues().getLast().date());
        }
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void ptfProgress_corpActions() {
        FinTracker finTracker = createFinTracker("dms-stable");
        IbkrModule ibkrModule = finTracker.get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = finTracker.validator();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2023-08-02"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("43.659223735")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("0.402378700")));

            assertEquals(15, ptf.getHoldingsCount());
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2023-11-17"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("722.811854405")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("183.601774170")));

            assertEquals(23, ptf.getHoldingsCount());
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2023-11-29"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("722.811854405")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("1071.101774170")));

            assertEquals(23, ptf.getHoldingsCount());
            assertEquals(0, ptf.getHoldingQty(US, "VMW").compareTo(ZERO));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-04-30"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("284.92")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("164.64")));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-05-31"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("130.13")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("54.07")));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-06-05"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("1090.13")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("64.89")));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-06-10"));
            for (FinTransaction finTran : ptfProgress.transactions()) {
                Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
                assertEquals(0, violations.size(), () -> "%s, %s".formatted(violations, finTran));
            }
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getHoldingQty(US, "NVDA").compareTo(new BigDecimal("60")));
        }
    }

}

