package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

@SuppressWarnings("DuplicatedCode")
public class TrueTwrCalculator {

    public static BigDecimal calculateTrueTwrReturn(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            Map<LocalDate, BigDecimal> assetValues,
            SortedMap<LocalDate, BigDecimal> flows,
            FlowTiming flowTiming,
            AnnualizationOption annualizationOption,
            int calcScale,
            RoundingMode roundingMode
    ) {

        BigDecimal cumulFactor = switch (flowTiming) {
            case BEGINNING_OF_DAY -> calculateCumulTwrFactorWithFlowsAtBeginningOfDay(
                    startDateIncl,
                    endDateIncl,
                    flows,
                    assetValues,
                    calcScale,
                    roundingMode
            );
            case END_OF_DAY -> calculateCumulTwrFactorWithFlowsAtEndOfDay(
                    startDateIncl,
                    endDateIncl,
                    flows,
                    assetValues,
                    calcScale,
                    roundingMode
            );
        };
        BigDecimal annFactor = AnnualizationUtil.annualizeGrowthFactor(annualizationOption, cumulFactor, startDateIncl, endDateIncl);
        return annFactor.subtract(ONE);
    }

    private static BigDecimal calculateCumulTwrFactorWithFlowsAtBeginningOfDay(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            SortedMap<LocalDate, BigDecimal> cashFlows,
            Map<LocalDate, BigDecimal> assetValues,
            int calcScale,
            RoundingMode roundingMode
    ) {
        List<LocalDate> subPeriodDates = new ArrayList<>();
        subPeriodDates.add(startDateIncl);
        subPeriodDates.addAll(cashFlows.subMap(startDateIncl.plusDays(1), endDateIncl.plusDays(1)).keySet());
        subPeriodDates.add(endDateIncl.plusDays(1));

        BigDecimal cumulGrowthFactor = ONE;
        for (int i = 1, subPeriodDatesSize = subPeriodDates.size(); i < subPeriodDatesSize; i++) {
            LocalDate subPeriodStartDateIncl = subPeriodDates.get(i - 1);
            LocalDate subPeriodStartDateExcl = subPeriodStartDateIncl.minusDays(1);
            LocalDate subPeriodEndDateIncl = subPeriodDates.get(i).minusDays(1);

            BigDecimal subPeriodStartValue = assetValues.get(subPeriodStartDateExcl);
            BigDecimal subPeriodEndValue = assetValues.get(subPeriodEndDateIncl);
            Validate.notNull(subPeriodStartValue,
                    () -> "subPeriodStartValue must not be null, missing assetValue for subPeriodStartDateExcl %s".formatted(subPeriodStartDateExcl));
            Validate.notNull(subPeriodEndValue,
                    () -> "subPeriodEndValue must not be null, missing assetValue for subPeriodEndDateIncl %s".formatted(subPeriodEndDateIncl));
            BigDecimal flow = cashFlows.getOrDefault(subPeriodStartDateIncl, ZERO);
            BigDecimal subPeriodStartValueWithFlow = subPeriodStartValue.add(flow);

            BigDecimal periodFactor;
            if (subPeriodStartValueWithFlow.compareTo(ZERO) == 0) {
                if (subPeriodEndValue.compareTo(ZERO) == 0) {
                    periodFactor = ONE;
                } else {
                    throw new IllegalArgumentException((
                            "if subPeriodStartValueWithFlow is zero, then subPeriodEndValue must be zero; " +
                            "given: subPeriodStartValueWithFlow=%s, subPeriodEndValue=%s, subPeriodStartDateExcl=%s, subPeriodEndDateIncl=%s")
                            .formatted(subPeriodStartValueWithFlow, subPeriodEndValue, subPeriodStartDateExcl, subPeriodEndDateIncl));
                }
            } else {
                periodFactor = subPeriodEndValue.divide(subPeriodStartValueWithFlow, calcScale, roundingMode);
                int periodFactorSignum = periodFactor.signum();
                if (periodFactorSignum == 0) {
                    cumulGrowthFactor = ZERO;
                    break;
                } else {
                    Validate.isTrue(periodFactorSignum > 0);
                }
            }
            cumulGrowthFactor = cumulGrowthFactor.multiply(periodFactor).setScale(calcScale, roundingMode);
        }

        return cumulGrowthFactor;
    }

    private static BigDecimal calculateCumulTwrFactorWithFlowsAtEndOfDay(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            SortedMap<LocalDate, BigDecimal> cashFlows,
            Map<LocalDate, BigDecimal> assetValues,
            int calcScale,
            RoundingMode roundingMode
    ) {
        List<LocalDate> subPeriodDates = new ArrayList<>();
        subPeriodDates.add(startDateIncl.minusDays(1));
        subPeriodDates.addAll(cashFlows.subMap(startDateIncl, endDateIncl).keySet());
        subPeriodDates.add(endDateIncl);

        BigDecimal cumulGrowthFactor = ONE;
        for (int i = 1, subPeriodDatesSize = subPeriodDates.size(); i < subPeriodDatesSize; i++) {
            LocalDate subPeriodStartDateExcl = subPeriodDates.get(i - 1);
            LocalDate subPeriodEndDateIncl = subPeriodDates.get(i);

            BigDecimal subPeriodStartValue = assetValues.get(subPeriodStartDateExcl);
            BigDecimal subPeriodEndValue = assetValues.get(subPeriodEndDateIncl);
            Validate.notNull(subPeriodStartValue,
                    () -> "subPeriodStartValue must not be null, missing assetValue for subPeriodStartDateExcl %s".formatted(subPeriodStartDateExcl));
            Validate.notNull(subPeriodEndValue,
                    () -> "subPeriodEndValue must not be null, missing assetValue for subPeriodEndDateIncl %s".formatted(subPeriodEndDateIncl));

            BigDecimal flow = cashFlows.getOrDefault(subPeriodEndDateIncl, ZERO);
            BigDecimal subPeriodEndValueWithoutFlow = subPeriodEndValue.subtract(flow);

            BigDecimal periodFactor;
            if (subPeriodStartValue.compareTo(ZERO) == 0) {
                if (subPeriodEndValueWithoutFlow.compareTo(ZERO) == 0) {
                    periodFactor = ONE;
                } else {
                    throw new IllegalArgumentException((
                            "if subPeriodStartValue is zero, then subPeriodEndValueWithoutFlow must be zero; " +
                            "given: subPeriodStartValue=%s, subPeriodEndValueWithoutFlow=%s, subPeriodStartDateExcl=%s, subPeriodEndDateIncl=%s")
                            .formatted(subPeriodStartValue, subPeriodEndValueWithoutFlow, subPeriodStartDateExcl, subPeriodEndDateIncl));
                }
            } else {
                periodFactor = subPeriodEndValueWithoutFlow.divide(subPeriodStartValue, calcScale, roundingMode);
                int periodFactorSignum = periodFactor.signum();
                if (periodFactorSignum == 0) {
                    cumulGrowthFactor = ZERO;
                    break;
                } else {
                    Validate.isTrue(periodFactorSignum > 0);
                }
            }
            cumulGrowthFactor = cumulGrowthFactor.multiply(periodFactor).setScale(calcScale, roundingMode);
        }

        return cumulGrowthFactor;
    }
}
