package com.brinvex.fintracker.performancecalc.impl.service;

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
import java.util.SortedMap;
import java.util.TreeMap;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("DuplicatedCode")
public class TwrCalculator {

    public static BigDecimal calculateTwrReturn(RateOfReturnCalcRequest mwrCalcReq) {
        return calculateTwrReturn(
                mwrCalcReq.periodStartDateIncl(),
                mwrCalcReq.periodEndDateIncl(),
                mwrCalcReq.startValueExcl(),
                mwrCalcReq.endValueIncl(),
                mwrCalcReq.cashFlows(),
                mwrCalcReq.assetValues(),
                mwrCalcReq.flowTiming(),
                mwrCalcReq.annualize()
        );
    }

    public static BigDecimal calculateTwrReturn(
            LocalDate beginDateIncl,
            LocalDate endDateIncl,
            BigDecimal beginValueExcl,
            BigDecimal endValueIncl,
            Collection<DateAmount> cashFlows,
            Collection<DateAmount> assetValues,
            FlowTiming flowTiming,
            boolean annualize
    ) {

        SortedMap<LocalDate, BigDecimal> sortedFlows = cashFlows
                .stream()
                .sorted(comparing(DateAmount::date))
                .collect(toMap(DateAmount::date, DateAmount::amount, BigDecimal::add, TreeMap::new));
        if (!sortedFlows.isEmpty()) {
            Validate.isTrue(!sortedFlows.firstKey().isBefore(beginDateIncl), () -> "Flow date must not be before the beginDateIncl, given: %s %s"
                    .formatted(sortedFlows.firstKey(), beginDateIncl));
            Validate.isTrue(!sortedFlows.lastKey().isAfter(endDateIncl), () -> "Flow date must not be after the endDateIncl, given: %s %s"
                    .formatted(sortedFlows.lastKey(), endDateIncl));
        }

        SortedMap<LocalDate, BigDecimal> sortedValues = assetValues
                .stream()
                .sorted(comparing(DateAmount::date))
                .collect(toMap(DateAmount::date, DateAmount::amount, BigDecimal::add, TreeMap::new));
        if (!sortedValues.isEmpty()) {
            Validate.isTrue(!sortedValues.firstKey().isBefore(beginDateIncl), () -> "Asset value date must not be before the beginDateIncl, given: %s %s"
                    .formatted(sortedFlows.firstKey(), beginDateIncl));
            Validate.isTrue(!sortedValues.lastKey().isAfter(endDateIncl), () -> "Asset value date must not be after the endDateIncl, given: %s %s"
                    .formatted(sortedFlows.lastKey(), endDateIncl));
        }
        sortedValues.put(beginDateIncl.minusDays(1), beginValueExcl);
        sortedValues.put(endDateIncl, endValueIncl);

        BigDecimal cumulGrowthFactor = switch (flowTiming) {
            case BEGINNING_OF_DAY -> calculateCumulTwrFactorWithFlowsAtBeginningOfDay(
                    beginDateIncl,
                    endDateIncl,
                    sortedFlows,
                    sortedValues
            );
            case END_OF_DAY -> calculateCumulTwrFactorWithFlowsAtEndOfDay(
                    beginDateIncl,
                    endDateIncl,
                    sortedFlows,
                    sortedValues
            );
        };

        if (annualize) {
            return CalcUtil.annualizeGrowthFactor(cumulGrowthFactor, beginDateIncl, endDateIncl).subtract(ONE);
        } else {
            return cumulGrowthFactor.subtract(ONE);
        }
    }

    private static BigDecimal calculateCumulTwrFactorWithFlowsAtBeginningOfDay(
            LocalDate beginDateIncl,
            LocalDate endDateIncl,
            SortedMap<LocalDate, BigDecimal> cashFlows,
            SortedMap<LocalDate, BigDecimal> assetValues
    ) {
        List<LocalDate> subPeriodDates = new ArrayList<>();
        subPeriodDates.add(beginDateIncl);
        subPeriodDates.addAll(cashFlows.subMap(beginDateIncl.plusDays(1), endDateIncl.plusDays(1)).keySet());
        subPeriodDates.add(endDateIncl.plusDays(1));

        BigDecimal cumulGrowthFactor = ONE;
        for (int i = 1, subPeriodDatesSize = subPeriodDates.size(); i < subPeriodDatesSize; i++) {
            LocalDate subPeriodBeginDateIncl = subPeriodDates.get(i - 1);
            LocalDate subPeriodBeginDateExcl = subPeriodBeginDateIncl.minusDays(1);
            LocalDate subPeriodEndDateIncl = subPeriodDates.get(i).minusDays(1);

            BigDecimal subPeriodBeginValue = assetValues.get(subPeriodBeginDateExcl);
            BigDecimal subPeriodEndValue = assetValues.get(subPeriodEndDateIncl);
            Validate.notNull(subPeriodBeginValue,
                    () -> "subPeriodBeginValue must not be null, missing assetValue at subPeriodBeginDateExcl %s".formatted(subPeriodBeginDateExcl));
            Validate.notNull(subPeriodEndValue,
                    () -> "subPeriodEndValue must not be null, missing assetValue at subPeriodEndDateIncl %s".formatted(subPeriodEndDateIncl));
            BigDecimal flow = requireNonNullElse(cashFlows.get(subPeriodBeginDateIncl), ZERO);
            BigDecimal subPeriodBeginValueWithFlow = subPeriodBeginValue.add(flow);

            BigDecimal periodFactor = subPeriodEndValue.divide(subPeriodBeginValueWithFlow, 8, RoundingMode.HALF_UP);

            cumulGrowthFactor = cumulGrowthFactor.multiply(periodFactor);
        }

        return cumulGrowthFactor;
    }

    private static BigDecimal calculateCumulTwrFactorWithFlowsAtEndOfDay(
            LocalDate beginDateIncl,
            LocalDate endDateIncl,
            SortedMap<LocalDate, BigDecimal> cashFlows,
            SortedMap<LocalDate, BigDecimal> assetValues
    ) {
        List<LocalDate> subPeriodDates = new ArrayList<>();
        subPeriodDates.add(beginDateIncl.minusDays(1));
        subPeriodDates.addAll(cashFlows.subMap(beginDateIncl, endDateIncl).keySet());
        subPeriodDates.add(endDateIncl);

        BigDecimal cumulGrowthFactor = ONE;
        for (int i = 1, subPeriodDatesSize = subPeriodDates.size(); i < subPeriodDatesSize; i++) {
            LocalDate subPeriodBeginDateExcl = subPeriodDates.get(i - 1);
            LocalDate subPeriodEndDateIncl = subPeriodDates.get(i);

            BigDecimal subPeriodBeginValue = assetValues.get(subPeriodBeginDateExcl);
            BigDecimal subPeriodEndValue = assetValues.get(subPeriodEndDateIncl);
            Validate.notNull(subPeriodBeginValue,
                    () -> "subPeriodBeginValue must not be null, missing assetValue at subPeriodBeginDateExcl %s".formatted(subPeriodBeginDateExcl));
            Validate.notNull(subPeriodEndValue,
                    () -> "subPeriodEndValue must not be null, missing assetValue at subPeriodEndDateIncl %s".formatted(subPeriodEndDateIncl));

            BigDecimal flow = requireNonNullElse(cashFlows.get(subPeriodEndDateIncl), ZERO);
            BigDecimal subPeriodEndWithoutFlow = subPeriodEndValue.subtract(flow);

            BigDecimal periodFactor = subPeriodEndWithoutFlow.divide(subPeriodBeginValue, 8, RoundingMode.HALF_UP);

            cumulGrowthFactor = cumulGrowthFactor.multiply(periodFactor);
        }

        return cumulGrowthFactor;
    }
}
