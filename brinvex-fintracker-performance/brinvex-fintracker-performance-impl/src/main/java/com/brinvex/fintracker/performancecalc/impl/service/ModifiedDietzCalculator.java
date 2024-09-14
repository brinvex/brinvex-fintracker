package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.exception.CalculationException;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcRequest;
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

    public static BigDecimal calculate(RateOfReturnCalcRequest mwrCalcReq) {
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
        BigDecimal adjBeginValueExcl;
        LocalDate adjBeginDateIncl;
        if (beginValueExcl.compareTo(ZERO) == 0) {
            if (cashFlows.isEmpty()) {
                Validate.isTrue(endValueIncl.compareTo(ZERO) == 0);
                return ZERO;
            } else {
                List<DateAmount> sortedCashFlows = cashFlows
                        .stream()
                        .sorted(comparing(DateAmount::date))
                        .collect(toCollection(ArrayList::new));
                LocalDate firstCashFlowDate = sortedCashFlows.getFirst().date();
                int firstDateCashFlowsCount = toIntExact(sortedCashFlows
                        .stream()
                        .map(DateAmount::date)
                        .takeWhile(d -> d.isEqual(firstCashFlowDate))
                        .count());
                adjBeginValueExcl = sortedCashFlows.subList(0, firstDateCashFlowsCount)
                        .stream()
                        .map(DateAmount::amount)
                        .reduce(ZERO, BigDecimal::add);
                cashFlows = sortedCashFlows.subList(firstDateCashFlowsCount, sortedCashFlows.size());
                switch (flowTiming) {
                    case BEGINNING_OF_DAY -> adjBeginDateIncl = firstCashFlowDate;
                    case END_OF_DAY -> {
                        int firstCashFlowDateToEndDateComp = firstCashFlowDate.compareTo(endDateIncl);
                        if (firstCashFlowDateToEndDateComp == 0) {
                            return ZERO;
                        } else if (firstCashFlowDateToEndDateComp < 0) {
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
            adjBeginValueExcl = beginValueExcl;
            adjBeginDateIncl = beginDateIncl;
        }

        BigDecimal adjEndValueIncl;
        LocalDate adjEndDateIncl;
        if (endValueIncl.compareTo(ZERO) == 0) {
            if (cashFlows.isEmpty()) {
                Validate.isTrue(adjBeginValueExcl.compareTo(ZERO) == 0);
                return ZERO;
            } else {
                List<DateAmount> sortedCashFlows = cashFlows
                        .stream()
                        .sorted(comparing(DateAmount::date))
                        .collect(toCollection(ArrayList::new));
                LocalDate lastCashFlowDate = sortedCashFlows.getLast().date();
                int nonLastDateCashFlowsCount = toIntExact(sortedCashFlows
                        .stream()
                        .map(DateAmount::date)
                        .takeWhile(d -> !d.isEqual(lastCashFlowDate))
                        .count());
                adjEndValueIncl = sortedCashFlows.subList(nonLastDateCashFlowsCount, sortedCashFlows.size())
                        .stream()
                        .map(DateAmount::amount)
                        .reduce(ZERO, BigDecimal::add)
                        .negate();
                cashFlows = sortedCashFlows.subList(0, nonLastDateCashFlowsCount);
                switch (flowTiming) {
                    case BEGINNING_OF_DAY -> {
                        int lastCashFlowDateToAdjBeginDateComp = lastCashFlowDate.compareTo(adjBeginDateIncl);
                        if (lastCashFlowDateToAdjBeginDateComp == 0) {
                            return ZERO;
                        } else if (lastCashFlowDateToAdjBeginDateComp > 0) {
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
            adjEndValueIncl = endValueIncl;
            adjEndDateIncl = endDateIncl;
        }

        Validate.isTrue(adjBeginValueExcl.compareTo(ZERO) > 0, () -> "adjBeginValueExcl must be greater than zero, given: %s"
                .formatted(adjBeginValueExcl));
        Validate.isTrue(adjEndValueIncl.compareTo(ZERO) > 0, () -> "adjEndValueIncl must be greater than zero, given: %s"
                .formatted(adjEndValueIncl));

        BigDecimal totalDays = new BigDecimal(DAYS.between(adjBeginDateIncl, adjEndDateIncl) + 1);

        BigDecimal cashFlowSum = ZERO;
        BigDecimal weightedCashFlowSum = ZERO;
        for (DateAmount cashFlow : cashFlows) {
            LocalDate cashFlowDate = cashFlow.date();
            BigDecimal cashFlowValue = cashFlow.amount();
            Validate.isTrue(!cashFlowDate.isBefore(adjBeginDateIncl), () -> "cashFlowDate must not be before adjBeginDateIncl, given: %s, %s"
                    .formatted(cashFlowDate, adjBeginDateIncl));
            Validate.isTrue(!cashFlowDate.isAfter(adjEndDateIncl), () -> "cashFlowDate must not be after adjEndDateIncl, given: %s, %s"
                    .formatted(cashFlowDate, adjEndDateIncl));

            int cashFlowLagInDays = toIntExact(DAYS.between(adjBeginDateIncl, cashFlowDate)) + switch (flowTiming) {
                case BEGINNING_OF_DAY -> 0;
                case END_OF_DAY -> 1;
            };
            BigDecimal weight = ONE.subtract(new BigDecimal(cashFlowLagInDays).divide(totalDays, 20, RoundingMode.HALF_UP));
            BigDecimal weightedCashFlowValue = cashFlowValue.multiply(weight);

            cashFlowSum = cashFlowSum.add(cashFlowValue);
            weightedCashFlowSum = weightedCashFlowSum.add(weightedCashFlowValue);
        }
        if (adjBeginValueExcl.compareTo(weightedCashFlowSum.negate()) <= 0) {
            //See https://en.wikipedia.org/wiki/Modified_Dietz_method#Negative_or_zero_average_capital
            throw new CalculationException((
                    "Could not calculate ModifiedDietz return of given data: " +
                    "adjBeginValueExcl=%s, adjEndValueIncl=%s, " +
                    "weightedCashFlowSum=%s, cashFlowSum=%s, " +
                    "beginDateIncl=%s, endDateIncl=%s")
                    .formatted(
                            adjBeginValueExcl, adjEndValueIncl,
                            weightedCashFlowSum, cashFlows,
                            beginDateIncl, endDateIncl
                    ));
        }

        BigDecimal gain = adjEndValueIncl.subtract(adjBeginValueExcl).subtract(cashFlowSum);
        BigDecimal averageCapital = adjBeginValueExcl.add(weightedCashFlowSum);

        BigDecimal cumulativeReturn = gain.divide(averageCapital, 20, RoundingMode.HALF_UP);
        if (annualize) {
            return CalcUtil.annualizeReturn(cumulativeReturn, beginDateIncl, endDateIncl);
        } else {
            return cumulativeReturn;
        }
    }
}
