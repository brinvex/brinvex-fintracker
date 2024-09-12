package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.MwrCalcRequest;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.toIntExact;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;

public class ModifiedDietzCalculator {

    public static BigDecimal calculate(MwrCalcRequest mwrCalcReq) {
        return calculateMwrReturn(
                mwrCalcReq.periodStartDateIncl(),
                mwrCalcReq.periodEndDateIncl(),
                mwrCalcReq.startValueExcl(),
                mwrCalcReq.endValueIncl(),
                mwrCalcReq.cashFlows(),
                mwrCalcReq.flowTiming(),
                mwrCalcReq.annualize()
        );
    }

    public static BigDecimal calculateMwrReturn(
            LocalDate beginDateIncl,
            LocalDate endDateIncl,
            BigDecimal beginValueExcl,
            BigDecimal endValueIncl,
            Collection<DateAmount> cashFlows,
            FlowTiming flowTiming,
            boolean annualize
    ) {
        LocalDate adjBeginDateIncl;
        if (beginValueExcl.compareTo(ZERO) == 0) {
            if (cashFlows.isEmpty()) {
                Validate.isTrue(endValueIncl.compareTo(ZERO) == 0);
                return ZERO;
            } else {
                List<DateAmount> ajdCashFlows = cashFlows.stream().sorted(comparing(DateAmount::date)).collect(toCollection(ArrayList::new));
                DateAmount firstCashFlow = ajdCashFlows.removeFirst();
                Validate.isTrue(firstCashFlow.amount().compareTo(ZERO) > 0);
                cashFlows = ajdCashFlows;
                beginValueExcl = firstCashFlow.amount();
                LocalDate firstCashFlowDate = firstCashFlow.date();
                switch (flowTiming) {
                    case BEGINNING_OF_DAY -> adjBeginDateIncl = firstCashFlowDate;
                    case END_OF_DAY -> {
                        int firstCashFlowDateComp = firstCashFlowDate.compareTo(endDateIncl);
                        if (firstCashFlowDateComp == 0) {
                            return ZERO;
                        } else if (firstCashFlowDateComp < 0) {
                            adjBeginDateIncl = firstCashFlowDate.plusDays(1);
                        } else {
                            throw new IllegalArgumentException(
                                    "if beginValueExcl is zero, then firstCashFlowDate must not be after endDateIncl, given: %s, %s"
                                            .formatted(firstCashFlowDate, endDateIncl));
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + flowTiming);
                }
            }
        } else {
            adjBeginDateIncl = beginDateIncl;
        }

        LocalDate adjEndDateIncl;
        if (endValueIncl.compareTo(ZERO) == 0) {
            if (cashFlows.isEmpty()) {
                Validate.isTrue(beginValueExcl.compareTo(ZERO) == 0);
                return ZERO;
            } else {
                List<DateAmount> ajdCashFlows = cashFlows.stream().sorted(comparing(DateAmount::date)).collect(toCollection(ArrayList::new));
                DateAmount lastCashFlow = ajdCashFlows.removeLast();
                Validate.isTrue(lastCashFlow.amount().compareTo(ZERO) < 0);
                cashFlows = ajdCashFlows;
                endValueIncl = lastCashFlow.amount().negate();
                LocalDate lastCashFlowDate = lastCashFlow.date();
                switch (flowTiming) {
                    case BEGINNING_OF_DAY -> {
                        int lastCashFlowDateComp = lastCashFlowDate.compareTo(adjBeginDateIncl);
                        if (lastCashFlowDateComp == 0) {
                            return ZERO;
                        } else if (lastCashFlowDateComp > 0) {
                            adjEndDateIncl = lastCashFlowDate.minusDays(1);
                        } else {
                            throw new IllegalArgumentException(
                                    "if endValueExcl is zero, then lastCashFlowDate must not be before adjBeginDateIncl, given: %s, %s"
                                            .formatted(lastCashFlowDate, adjBeginDateIncl));
                        }
                    }
                    case END_OF_DAY -> adjEndDateIncl = lastCashFlowDate;
                    default -> throw new IllegalStateException("Unexpected value: " + flowTiming);
                }
            }
        } else {
            adjEndDateIncl = endDateIncl;
        }

        BigDecimal totalDays = new BigDecimal(DAYS.between(adjBeginDateIncl, adjEndDateIncl) + 1);

        BigDecimal cashFlowSum = ZERO;
        BigDecimal weightedCashFlowSum = ZERO;
        for (DateAmount cashFlow : cashFlows) {
            LocalDate cashFlowDate = cashFlow.date();
            BigDecimal cashFlowValue = cashFlow.amount();
            Validate.isTrue(!cashFlowDate.isBefore(adjBeginDateIncl));
            Validate.isTrue(!cashFlowDate.isAfter(adjEndDateIncl));


            int cashFlowLagInDays = toIntExact(DAYS.between(adjBeginDateIncl, cashFlowDate) + 1);
            cashFlowLagInDays = switch (flowTiming) {
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
