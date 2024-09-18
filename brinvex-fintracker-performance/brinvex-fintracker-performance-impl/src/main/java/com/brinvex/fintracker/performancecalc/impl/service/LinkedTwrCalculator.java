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
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeGrowthFactor;
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
            SortedMap<LocalDate, BigDecimal> assetValues,
            SortedMap<LocalDate, BigDecimal> flows,
            FlowTiming flowTiming,
            AnnualizationOption annualization,
            int calcScale,
            RoundingMode roundingMode
    ) {
        PeriodUnit periodUnit = PeriodUnit.MONTH;

        LocalDate subPeriodStartDateIncl = startDateIncl;
        BigDecimal cumulTwrFactor = ONE;
        while (!subPeriodStartDateIncl.isAfter(endDateIncl)) {
            LocalDate subPeriodStartDateExcl = subPeriodStartDateIncl.minusDays(1);
            LocalDate subPeriodEndDateIncl = minDate(periodUnit.adjEndDateIncl(subPeriodStartDateIncl), endDateIncl);
            LocalDate subPeriodEndDateExcl = subPeriodEndDateIncl.plusDays(1);
            BigDecimal subPeriodStartValueExcl = assetValues.get(subPeriodStartDateExcl);
            Validate.notNull(subPeriodStartValueExcl, () -> "subPeriodStartValueExcl must not be null, missing asset value for subPeriodStartDateExcl=%s"
                    .formatted(subPeriodStartDateExcl));
            BigDecimal subPeriodEndValueIncl = assetValues.get(subPeriodEndDateIncl);
            Validate.notNull(subPeriodEndValueIncl, () -> "subPeriodEndValueIncl must not be null, missing asset value for endDateIncl=%s"
                    .formatted(subPeriodEndDateIncl));

            SortedMap<LocalDate, BigDecimal> subPeriodFlows = flows.subMap(subPeriodStartDateIncl, subPeriodEndDateExcl);
            SortedMap<LocalDate, BigDecimal> subPeriodAssetValues = assetValues.subMap(subPeriodStartDateExcl, subPeriodEndDateExcl);

            BigDecimal subPeriodFactor = ONE.add(subPeriodReturnCalculator.apply(PerfCalcRequest.builder()
                    .calcMethod(subPeriodCalcMethod)
                    .startDateIncl(subPeriodStartDateIncl)
                    .endDateIncl(subPeriodEndDateIncl)
                    .startAssetValueExcl(subPeriodStartValueExcl)
                    .endAssetValueIncl(subPeriodEndValueIncl)
                    .flows(subPeriodFlows)
                    .assetValues(subPeriodAssetValues)
                    .flowTiming(flowTiming)
                    .annualization(DO_NOT_ANNUALIZE)
                    .calcScale(calcScale)
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
        }
        return annualizeGrowthFactor(annualization, cumulTwrFactor, startDateIncl, endDateIncl).subtract(ONE);
    }
}
