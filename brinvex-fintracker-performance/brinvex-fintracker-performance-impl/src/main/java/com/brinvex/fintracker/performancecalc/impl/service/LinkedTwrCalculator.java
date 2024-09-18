package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.util.java.DateUtil.minDate;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

@SuppressWarnings("DuplicatedCode")
public class LinkedTwrCalculator {

    public static BigDecimal calculateLinkedTwrReturn(
            RateOfReturnCalcMethod subPeriodCalcMethod,
            Function<PerfCalcRequest, BigDecimal> subPeriodReturnCalculator,
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            Map<LocalDate, BigDecimal> assetValues,
            SortedMap<LocalDate, BigDecimal> flows,
            FlowTiming flowTiming,
            AnnualizationOption annualizationOption,
            int calcScale,
            RoundingMode roundingMode
    ) {
        PeriodUnit periodUnit = PeriodUnit.MONTH;

        LocalDate periodStartDateIncl = startDateIncl;
        BigDecimal cumulTwrFactor = ONE;
        while (!periodStartDateIncl.isAfter(endDateIncl)) {
            LocalDate periodStartDateExcl = periodStartDateIncl.minusDays(1);
            LocalDate periodEndDateIncl = minDate(periodUnit.adjEndDateIncl(periodStartDateIncl), endDateIncl);
            LocalDate periodEndDateExcl = periodEndDateIncl.plusDays(1);
            BigDecimal periodStartValueExcl = assetValues.get(periodStartDateExcl);
            Validate.notNull(periodStartValueExcl, () -> "periodStartValueExcl must not be null, missing asset value for periodStartDateExcl=%s"
                    .formatted(periodStartDateExcl));
            BigDecimal periodEndValueIncl = assetValues.get(endDateIncl);
            Validate.notNull(periodEndValueIncl, () -> "periodEndValueIncl must not be null, missing asset value for endDateIncl=%s"
                    .formatted(periodEndDateIncl));

            SortedMap<LocalDate, BigDecimal> periodFlows = flows.subMap(periodStartDateIncl, periodEndDateExcl);

            BigDecimal subPeriodFactor = ONE.add(subPeriodReturnCalculator.apply(PerfCalcRequest.builder()
                    .calcMethod(subPeriodCalcMethod)
                    .startDateIncl(periodStartDateExcl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(periodStartValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(periodFlows)
                    .assetValues(assetValues)
                    .flowTiming(flowTiming)
                    .annualization(DO_NOT_ANNUALIZE)
                    .calcScale(calcScale)
                    .roundingMode(roundingMode)
                    .build()));

            int periodFactorSignum = subPeriodFactor.signum();
            if (periodFactorSignum == 0) {
                cumulTwrFactor = ZERO;
                break;
            } else {
                Validate.isTrue(periodFactorSignum > 0);
            }

            cumulTwrFactor = cumulTwrFactor.multiply(subPeriodFactor).setScale(calcScale, roundingMode);

            periodStartDateIncl = periodEndDateIncl.plusDays(1);
        }
        return AnnualizationUtil.annualizeGrowthFactor(annualizationOption, cumulTwrFactor, startDateIncl, endDateIncl).subtract(ONE);
    }
}
