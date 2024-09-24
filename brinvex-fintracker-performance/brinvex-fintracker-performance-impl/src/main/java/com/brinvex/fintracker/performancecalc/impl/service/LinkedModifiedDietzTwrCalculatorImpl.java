package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.SequencedMap;
import java.util.function.Function;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
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
        return calculateLinkedTwrCumulReturn(
                modifiedDietzMwrCalculator::calculateReturn,
                calcReq.startDateIncl(),
                calcReq.endDateIncl(),
                calcReq.assetValues(),
                calcReq.flows(),
                calcReq.flowTiming(),
                calcReq.calcScale(),
                calcReq.roundingMode());
    }

    private static BigDecimal calculateLinkedTwrCumulReturn(
            Function<PerfCalcRequest, BigDecimal> subPeriodReturnCalculator,
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            SequencedMap<LocalDate, BigDecimal> assetValues,
            SequencedMap<LocalDate, BigDecimal> flows,
            FlowTiming flowTiming,
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

            BigDecimal subPeriodFactor = ONE.add(subPeriodReturnCalculator.apply(PerfCalcRequest.builder()
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
        }
        return cumulTwrFactor.subtract(ONE);
    }
}
