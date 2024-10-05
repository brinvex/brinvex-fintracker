package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.CollectionUtil;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import static com.brinvex.util.java.CollectionUtil.rangeSafeHeadMap;
import static com.brinvex.util.java.CollectionUtil.rangeSafeTailMap;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

@SuppressWarnings("DuplicatedCode")
public class TrueTwrCalculatorImpl extends BaseCalculatorImpl implements PerformanceCalculator.TrueTwrCalculator {

    @Override
    protected BigDecimal calculateCumulativeReturn(PerfCalcRequest calcReq) {
        return calculateTrueTwrCumulReturn(
                calcReq.startDateIncl(),
                calcReq.endDateIncl(),
                calcReq.startAssetValueExcl(),
                calcReq.endAssetValueIncl(),
                calcReq.assetValues(),
                calcReq.flows(),
                calcReq.flowTiming(),
                calcReq.calcScale(),
                calcReq.roundingMode()
        );
    }

    private static BigDecimal calculateTrueTwrCumulReturn(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl,
            Map<LocalDate, BigDecimal> assetValues,
            SortedMap<LocalDate, BigDecimal> flows,
            FlowTiming flowTiming,
            int calcScale,
            RoundingMode roundingMode
    ) {

        BigDecimal cumulFactor = switch (flowTiming) {
            case BEGINNING_OF_DAY -> calculateCumulTwrFactorWithFlowsAtBeginningOfDay(
                    startDateIncl,
                    endDateIncl,
                    startAssetValueExcl,
                    endAssetValueIncl,
                    assetValues,
                    flows,
                    calcScale,
                    roundingMode
            );
            case END_OF_DAY -> calculateCumulTwrFactorWithFlowsAtEndOfDay(
                    startDateIncl,
                    endDateIncl,
                    startAssetValueExcl,
                    endAssetValueIncl,
                    assetValues,
                    flows,
                    calcScale,
                    roundingMode
            );
        };
        return cumulFactor.subtract(ONE);
    }

    private static BigDecimal calculateCumulTwrFactorWithFlowsAtBeginningOfDay(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl,
            Map<LocalDate, BigDecimal> assetValues,
            SortedMap<LocalDate, BigDecimal> flows,
            int calcScale,
            RoundingMode roundingMode
    ) {

        {
            Entry<LocalDate, BigDecimal> firstFlowEntry = flows.firstEntry();
            if (firstFlowEntry != null && firstFlowEntry.getKey().isEqual(startDateIncl)) {
                startAssetValueExcl = startAssetValueExcl.add(firstFlowEntry.getValue());
                flows = rangeSafeTailMap(flows, startDateIncl.plusDays(1));
            }
        }

        BigDecimal cumulGrowthFactor = ONE;

        LocalDate subPeriodStartDateIncl = startDateIncl;
        BigDecimal flow = ZERO;
        for (int i = 1, periodCount = flows.size() + 1; i <= periodCount; i++) {

            LocalDate subPeriodStartDateExcl = subPeriodStartDateIncl.minusDays(1);
            LocalDate subPeriodEndDateIncl;
            BigDecimal subPeriodStartValue;
            BigDecimal subPeriodEndValue;
            if (i == 1) {
                subPeriodStartValue = startAssetValueExcl;
            } else {
                subPeriodStartValue = assetValues.get(subPeriodStartDateExcl);

                Validate.notNull(subPeriodStartValue,
                        () -> "subPeriodStartValue must not be null, missing assetValue for subPeriodStartDateExcl %s".formatted(subPeriodStartDateExcl));
            }
            if (i == periodCount) {
                subPeriodEndDateIncl = endDateIncl;
                subPeriodEndValue = endAssetValueIncl;
            } else {
                subPeriodEndDateIncl = flows.firstKey().minusDays(1);
                subPeriodEndValue = assetValues.get(subPeriodEndDateIncl);

                Validate.notNull(subPeriodEndValue,
                        () -> "subPeriodEndValue must not be null, missing assetValue for subPeriodEndDateIncl %s".formatted(subPeriodEndDateIncl));
            }

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
                    //Bankruptcy
                    cumulGrowthFactor = ZERO;
                    break;
                } else {
                    Validate.isTrue(periodFactorSignum > 0);
                }
            }
            cumulGrowthFactor = cumulGrowthFactor.multiply(periodFactor).setScale(calcScale, roundingMode);

            // Variable values for the next iteration
            {
                subPeriodStartDateIncl = subPeriodEndDateIncl.plusDays(1);
                if (i < periodCount) {
                    flow = flows.firstEntry().getValue();
                    flows = rangeSafeTailMap(flows, subPeriodStartDateIncl.plusDays(1));
                }
            }
        }

        return cumulGrowthFactor;
    }

    private static BigDecimal calculateCumulTwrFactorWithFlowsAtEndOfDay(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl,
            Map<LocalDate, BigDecimal> assetValues,
            SortedMap<LocalDate, BigDecimal> flows,
            int calcScale,
            RoundingMode roundingMode
    ) {

        {
            Entry<LocalDate, BigDecimal> lastFlowEntry = flows.lastEntry();
            if (lastFlowEntry != null && lastFlowEntry.getKey().isEqual(endDateIncl)) {
                endAssetValueIncl = endAssetValueIncl.subtract(lastFlowEntry.getValue());
                flows = rangeSafeHeadMap(flows, endDateIncl);
            }
        }

        BigDecimal cumulGrowthFactor = ONE;

        LocalDate subPeriodStartDateIncl = startDateIncl;
        for (int i = 1, periodCount = flows.size() + 1; i <= periodCount; i++) {

            LocalDate subPeriodStartDateExcl = subPeriodStartDateIncl.minusDays(1);
            LocalDate subPeriodEndDateIncl;
            BigDecimal subPeriodStartValue;
            BigDecimal subPeriodEndValue;
            BigDecimal flow;
            if (i == 1) {
                subPeriodStartValue = startAssetValueExcl;
            } else {
                subPeriodStartValue = assetValues.get(subPeriodStartDateExcl);

                Validate.notNull(subPeriodStartValue,
                        () -> "subPeriodStartValue must not be null, missing assetValue for subPeriodStartDateExcl %s".formatted(subPeriodStartDateExcl));
            }
            if (i == periodCount) {
                flow = ZERO;
                subPeriodEndDateIncl = endDateIncl;
                subPeriodEndValue = endAssetValueIncl;
            } else {
                Entry<LocalDate, BigDecimal> flowEntry = flows.firstEntry();
                flow = flowEntry.getValue();
                subPeriodEndDateIncl = flowEntry.getKey();
                subPeriodEndValue = assetValues.get(subPeriodEndDateIncl);

                Validate.notNull(subPeriodEndValue,
                        () -> "subPeriodEndValue must not be null, missing assetValue for subPeriodEndDateIncl %s".formatted(subPeriodEndDateIncl));
            }

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
                    //Bankruptcy
                    cumulGrowthFactor = ZERO;
                    break;
                } else {
                    Validate.isTrue(periodFactorSignum > 0);
                }
            }
            cumulGrowthFactor = cumulGrowthFactor.multiply(periodFactor).setScale(calcScale, roundingMode);

            // Variable values for the next iteration
            {
                subPeriodStartDateIncl = subPeriodEndDateIncl.plusDays(1);
                if (i < periodCount) {
                    flows = rangeSafeTailMap(flows, subPeriodStartDateIncl.plusDays(1));
                }
            }
        }

        return cumulGrowthFactor;
    }
}
