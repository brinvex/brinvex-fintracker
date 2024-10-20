package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.Num;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.util.java.CollectionUtil.rangeSafeSubMap;
import static com.brinvex.util.java.DateUtil.minDate;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

@SuppressWarnings("DuplicatedCode")
public class LinkedModifiedDietzTwrCalculatorImpl extends BaseCalculatorImpl implements PerformanceCalculator.LinkedModifiedDietzTwrCalculator {

    private final ModifiedDietzMwrCalculator modifiedDietzMwrCalculator;

    public LinkedModifiedDietzTwrCalculatorImpl(ModifiedDietzMwrCalculator modifiedDietzMwrCalculator) {
        this.modifiedDietzMwrCalculator = modifiedDietzMwrCalculator;
    }

    @Override
    protected BigDecimal calculateCumulativeReturn(PerfCalcRequest calcReq) {
        LocalDate startDateIncl = calcReq.startDateIncl();
        LocalDate endDateIncl = calcReq.endDateIncl();
        BigDecimal startAssetValueExcl = calcReq.startAssetValueExcl();
        BigDecimal endAssetValueIncl = calcReq.endAssetValueIncl();
        Function<LocalDate, BigDecimal> assetValues = calcReq.assetValues();
        SortedMap<LocalDate, BigDecimal> flows = calcReq.flows();
        int largeFlowLevelInPercent = calcReq.largeFlowLevelInPercent();
        FlowTiming flowTiming = calcReq.flowTiming();
        int calcScale = calcReq.calcScale();
        RoundingMode roundingMode = calcReq.roundingMode();

        /*
        GIPS Standard
        Provision 22.A.20
        When calculating time-weighted returns, total funds and portfolios except private market investment portfolios must be valued:
        a. At least monthly.
        b. As of the calendar month end or the last business day of the month.
        c. On the date of all large cash flows.
         */
        PeriodUnit periodUnit = PeriodUnit.MONTH;

        LocalDate endDateExcl = endDateIncl.plusDays(1);
        LocalDate subPeriodStartDateIncl = startDateIncl;
        BigDecimal cumulTwrFactor = ONE;

        BigDecimal largeFlowLevel = new BigDecimal(largeFlowLevelInPercent).divide(Num._100, calcScale, roundingMode);

        SortedMap<LocalDate, BigDecimal> iterativeForwardFlows = flows;
        while (!subPeriodStartDateIncl.isAfter(endDateIncl)) {
            LocalDate subPeriodStartDateExcl = subPeriodStartDateIncl.minusDays(1);
            BigDecimal subPeriodStartValueExcl = subPeriodStartDateIncl == startDateIncl ? startAssetValueExcl : assetValues.apply(subPeriodStartDateExcl);
            if (subPeriodStartValueExcl == null) {
                throw new IllegalArgumentException("subPeriodStartValueExcl must not be null, missing assetValue for subPeriodStartDateExcl=%s"
                        .formatted(subPeriodStartDateExcl));
            }

            LocalDate subPeriodEndDateIncl = minDate(periodUnit.adjEndDateIncl(subPeriodStartDateIncl), endDateIncl);

            LocalDate largeFlowDate;
            if (subPeriodStartValueExcl.compareTo(ZERO) == 0) {
                largeFlowDate = null;
            } else {
                largeFlowDate = rangeSafeSubMap(iterativeForwardFlows, subPeriodStartDateIncl.plusDays(1), subPeriodEndDateIncl)
                        .entrySet()
                        .stream()
                        .filter(e -> {
                            BigDecimal flowLevel = e.getValue().divide(subPeriodStartValueExcl, calcScale, roundingMode).abs();
                            return flowLevel.compareTo(largeFlowLevel) > 0;
                        })
                        .findFirst()
                        .map(Map.Entry::getKey)
                        .orElse(null);
                if (largeFlowDate != null) {
                    subPeriodEndDateIncl = switch (flowTiming) {
                        case BEGINNING_OF_DAY -> largeFlowDate.minusDays(1);
                        case END_OF_DAY -> largeFlowDate;
                    };
                }
            }

            BigDecimal subPeriodEndValueIncl = subPeriodEndDateIncl == endDateIncl ? endAssetValueIncl : assetValues.apply(subPeriodEndDateIncl);
            if (subPeriodEndValueIncl == null) {
                throw new IllegalArgumentException((
                        "subPeriodEndValueIncl must not be null, missing assetValue for endDateIncl=%s, " +
                        "largeFlowDate=%s, flowTiming=%s"
                ).formatted(subPeriodEndDateIncl, largeFlowDate, flowTiming));
            }

            BigDecimal subPeriodFactor = ONE.add(modifiedDietzMwrCalculator.calculateReturn(PerfCalcRequest.builder()
                    .startDateIncl(subPeriodStartDateIncl)
                    .endDateIncl(subPeriodEndDateIncl)
                    .startAssetValueExcl(subPeriodStartValueExcl)
                    .endAssetValueIncl(subPeriodEndValueIncl)
                    .flows(flows)
                    .assetValues(assetValues)
                    .flowTiming(flowTiming)
                    .annualization(DO_NOT_ANNUALIZE)
                    .calcScale(calcScale)
                    .resultScale(calcScale)
                    .roundingMode(roundingMode)
                    .build()));

            int subPeriodFactorSignum = subPeriodFactor.signum();
            if (subPeriodFactorSignum == 0) {
                //Bankruptcy
                cumulTwrFactor = ZERO;
                break;
            } else {
                Validate.isTrue(subPeriodFactorSignum > 0);
            }

            cumulTwrFactor = cumulTwrFactor.multiply(subPeriodFactor).setScale(calcScale, roundingMode);

            subPeriodStartDateIncl = subPeriodEndDateIncl.plusDays(1);
            iterativeForwardFlows = rangeSafeSubMap(iterativeForwardFlows, subPeriodStartDateIncl, endDateExcl);
        }
        return cumulTwrFactor.subtract(ONE);
    }

}
