package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.exception.CalculationException;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.Num;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map.Entry;
import java.util.SortedMap;

import static java.lang.Math.toIntExact;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.time.temporal.ChronoUnit.DAYS;

public class ModifiedDietzMwrCalculatorImpl extends BaseCalculatorImpl implements PerformanceCalculator.ModifiedDietzMwrCalculator {

    @Override
    protected BigDecimal calculateCumulativeReturn(PerfCalcRequest calcReq) {
        return calculateModifiedDietzMwrCumulReturn(
                calcReq.startDateIncl(),
                calcReq.endDateIncl(),
                calcReq.startAssetValueExcl(),
                calcReq.endAssetValueIncl(),
                calcReq.flows(),
                calcReq.flowTiming(),
                calcReq.calcScale(),
                calcReq.roundingMode());
    }

    private static BigDecimal calculateModifiedDietzMwrCumulReturn(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            BigDecimal startValueExcl,
            BigDecimal endValueIncl,
            SortedMap<LocalDate, BigDecimal> flows,
            FlowTiming flowTiming,
            int calcScale,
            RoundingMode roundingMode
    ) {
        BigDecimal totalDays = new BigDecimal(DAYS.between(startDateIncl, endDateIncl) + 1);

        BigDecimal flowSum = ZERO;
        BigDecimal weightedFlowSum = ZERO;
        for (Entry<LocalDate, BigDecimal> cashFlow : flows.entrySet()) {
            LocalDate flowDate = cashFlow.getKey();
            BigDecimal flowValue = cashFlow.getValue();
            Validate.isTrue(!flowDate.isBefore(startDateIncl), () -> "flowDate must not be before startDateIncl, given: %s, %s"
                    .formatted(flowDate, startDateIncl));
            Validate.isTrue(!flowDate.isAfter(endDateIncl), () -> "flowDate must not be after endDateIncl, given: %s, %s"
                    .formatted(flowDate, endDateIncl));

            int cashFlowLagInDays = toIntExact(DAYS.between(startDateIncl, flowDate)) + switch (flowTiming) {
                case BEGINNING_OF_DAY -> 0;
                case END_OF_DAY -> 1;
            };
            BigDecimal weight = ONE.subtract(new BigDecimal(cashFlowLagInDays).divide(totalDays, calcScale, roundingMode));
            BigDecimal weightedCashFlowValue = flowValue.multiply(weight);

            flowSum = flowSum.add(flowValue);
            weightedFlowSum = weightedFlowSum.add(weightedCashFlowValue);
        }
        if (startValueExcl.compareTo(weightedFlowSum.negate()) <= 0) {
            //See https://en.wikipedia.org/wiki/Modified_Dietz_method#Negative_or_zero_average_capital
            throw new CalculationException((
                    "Could not calculate ModifiedDietz return of given data: " +
                    "adjStartValueExcl=%s, adjEndValueIncl=%s, " +
                    "weightedFlowSum=%s, periodFlow=%s, " +
                    "startDateIncl=%s, endDateIncl=%s")
                    .formatted(
                            startValueExcl, endValueIncl,
                            weightedFlowSum, flows,
                            startDateIncl, endDateIncl
                    ));
        }

        BigDecimal gain = endValueIncl.subtract(startValueExcl).subtract(flowSum);
        BigDecimal averageCapital = startValueExcl.add(weightedFlowSum);

        BigDecimal cumulReturn = gain.divide(averageCapital, calcScale, roundingMode);
        if (cumulReturn.compareTo(Num.MINUS_1) < 0) {
            cumulReturn = Num.MINUS_1;
        }
        return cumulReturn;
    }
}
