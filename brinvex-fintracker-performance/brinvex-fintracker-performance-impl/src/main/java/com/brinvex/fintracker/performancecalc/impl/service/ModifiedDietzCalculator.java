package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.model.CashFlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.MwrCalcRequest;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;

import static java.lang.Math.toIntExact;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.time.temporal.ChronoUnit.DAYS;

public class ModifiedDietzCalculator {

    public static BigDecimal calculate(MwrCalcRequest mwrCalcReq) {
        return calculateMwrReturn(
                mwrCalcReq.periodStartDateIncl(),
                mwrCalcReq.periodEndDateIncl(),
                mwrCalcReq.startValueExcl(),
                mwrCalcReq.endValueIncl(),
                mwrCalcReq.cashFlows(),
                mwrCalcReq.cashFlowTiming(),
                mwrCalcReq.annualize()
        );
    }

    public static BigDecimal calculateMwrReturn(
            LocalDate beginDateIncl,
            LocalDate endDateIncl,
            BigDecimal beginValueExcl,
            BigDecimal endValueIncl,
            Collection<DateAmount> cashFlows,
            CashFlowTiming cashFlowTiming,
            boolean annualize
    ) {
        BigDecimal totalDays = new BigDecimal(DAYS.between(beginDateIncl, endDateIncl) + 1);

        BigDecimal cashFlowSum = ZERO;
        BigDecimal weightedCashFlowSum = ZERO;
        for (DateAmount cashFlow : cashFlows) {
            LocalDate cashFlowDate = cashFlow.date();
            BigDecimal cashFlowValue = cashFlow.amount();
            Validate.isTrue(!cashFlowDate.isBefore(beginDateIncl));
            Validate.isTrue(!cashFlowDate.isAfter(endDateIncl));


            int cashFlowLagInDays = toIntExact(DAYS.between(beginDateIncl, cashFlowDate) + 1);
            cashFlowLagInDays = switch (cashFlowTiming) {
                case BEGINNING_OF_DAY -> cashFlowLagInDays - 1;
                case END_OF_DAY -> cashFlowLagInDays;
            };
            BigDecimal weight = ONE.subtract(new BigDecimal(cashFlowLagInDays).divide(totalDays, 20, RoundingMode.HALF_UP));
            BigDecimal weightedCashFlowValue = cashFlowValue.multiply(weight);

            cashFlowSum = cashFlowSum.add(cashFlowValue);
            weightedCashFlowSum = weightedCashFlowSum.add(weightedCashFlowValue);
        }

        BigDecimal numerator = endValueIncl.subtract(beginValueExcl).subtract(cashFlowSum);
        BigDecimal denominator = beginValueExcl.add(weightedCashFlowSum);

        BigDecimal cumulativeReturn = numerator.divide(denominator, 20, RoundingMode.HALF_UP);
        if (annualize) {
            return CalcUtil.annualizeReturn(cumulativeReturn, beginDateIncl, endDateIncl);
        } else {
            return cumulativeReturn;
        }
    }
}
