package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.exception.CalculationException;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map.Entry;
import java.util.SequencedMap;

import static com.brinvex.util.java.Collectors.toLinkedMap;
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
            SequencedMap<LocalDate, BigDecimal> flows,
            FlowTiming flowTiming,
            int calcScale,
            RoundingMode roundingMode
    ) {
        BigDecimal adjStartValueExcl;
        LocalDate adjStartDateIncl;
        if (startValueExcl.compareTo(ZERO) == 0) {
            if (flows.isEmpty()) {
                Validate.isTrue(endValueIncl.compareTo(ZERO) == 0);
                return ZERO;
            } else {
                Entry<LocalDate, BigDecimal> firstFlow = flows.firstEntry();
                adjStartValueExcl = firstFlow.getValue();
                LocalDate firstFlowDate = firstFlow.getKey();
                flows = flows.entrySet()
                        .stream()
                        .dropWhile(e -> !e.getKey().isAfter(firstFlowDate))
                        .collect(toLinkedMap(Entry::getKey, Entry::getValue));
                switch (flowTiming) {
                    case BEGINNING_OF_DAY -> adjStartDateIncl = firstFlowDate;
                    case END_OF_DAY -> {
                        int firstCashFlowDateToEndDateComp = firstFlowDate.compareTo(endDateIncl);
                        if (firstCashFlowDateToEndDateComp == 0) {
                            return ZERO;
                        } else if (firstCashFlowDateToEndDateComp < 0) {
                            adjStartDateIncl = firstFlowDate.plusDays(1);
                        } else {
                            throw new IllegalArgumentException(
                                    "if startAssetValueExcl is zero, then firstCashFlowDate must not be after endDateIncl, given: %s, %s"
                                            .formatted(firstFlowDate, endDateIncl));
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + flowTiming);
                }
            }
        } else {
            adjStartValueExcl = startValueExcl;
            adjStartDateIncl = startDateIncl;
        }
        if (adjStartValueExcl.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("adjStartValueExcl must be greater than zero, given: %s"
                    .formatted(adjStartValueExcl));
        }

        BigDecimal adjEndValueIncl;
        LocalDate adjEndDateIncl;
        if (endValueIncl.compareTo(ZERO) == 0 && !flows.isEmpty()) {
            Entry<LocalDate, BigDecimal> lastFlow = flows.lastEntry();
            adjEndValueIncl = lastFlow.getValue().negate();
            LocalDate lastFlowDate = lastFlow.getKey();
            flows = flows.entrySet()
                    .stream()
                    .takeWhile(e -> e.getKey().isBefore(lastFlowDate))
                    .collect(toLinkedMap(Entry::getKey, Entry::getValue));
            switch (flowTiming) {
                case BEGINNING_OF_DAY -> {
                    int lastCashFlowDateToAdjStartDateComp = lastFlowDate.compareTo(adjStartDateIncl);
                    if (lastCashFlowDateToAdjStartDateComp == 0) {
                        return ZERO;
                    } else if (lastCashFlowDateToAdjStartDateComp > 0) {
                        adjEndDateIncl = lastFlowDate.minusDays(1);
                    } else {
                        throw new IllegalArgumentException(
                                "if endValueExcl is zero, then lastCashFlowDate must not be before adjStartDateIncl, given: %s, %s"
                                        .formatted(lastFlowDate, adjStartDateIncl));
                    }
                }
                case END_OF_DAY -> adjEndDateIncl = lastFlowDate;
                default -> throw new IllegalStateException("Unexpected value: " + flowTiming);
            }
        } else {
            adjEndValueIncl = endValueIncl;
            adjEndDateIncl = endDateIncl;
        }

        if (adjEndValueIncl.compareTo(ZERO) <= 0) {
            //Bankruptcy
            return ONE.negate();
        }

        BigDecimal totalDays = new BigDecimal(DAYS.between(adjStartDateIncl, adjEndDateIncl) + 1);

        BigDecimal flowSum = ZERO;
        BigDecimal weightedFlowSum = ZERO;
        for (Entry<LocalDate, BigDecimal> cashFlow : flows.entrySet()) {
            LocalDate flowDate = cashFlow.getKey();
            BigDecimal flowValue = cashFlow.getValue();
            Validate.isTrue(!flowDate.isBefore(adjStartDateIncl), () -> "flowDate must not be before adjStartDateIncl, given: %s, %s"
                    .formatted(flowDate, adjStartDateIncl));
            Validate.isTrue(!flowDate.isAfter(adjEndDateIncl), () -> "flowDate must not be after adjEndDateIncl, given: %s, %s"
                    .formatted(flowDate, adjEndDateIncl));

            int cashFlowLagInDays = toIntExact(DAYS.between(adjStartDateIncl, flowDate)) + switch (flowTiming) {
                case BEGINNING_OF_DAY -> 0;
                case END_OF_DAY -> 1;
            };
            BigDecimal weight = ONE.subtract(new BigDecimal(cashFlowLagInDays).divide(totalDays, calcScale, roundingMode));
            BigDecimal weightedCashFlowValue = flowValue.multiply(weight);

            flowSum = flowSum.add(flowValue);
            weightedFlowSum = weightedFlowSum.add(weightedCashFlowValue);
        }
        if (adjStartValueExcl.compareTo(weightedFlowSum.negate()) <= 0) {
            //See https://en.wikipedia.org/wiki/Modified_Dietz_method#Negative_or_zero_average_capital
            throw new CalculationException((
                    "Could not calculate ModifiedDietz return of given data: " +
                    "adjStartValueExcl=%s, adjEndValueIncl=%s, " +
                    "weightedFlowSum=%s, periodFlow=%s, " +
                    "startDateIncl=%s, endDateIncl=%s")
                    .formatted(
                            adjStartValueExcl, adjEndValueIncl,
                            weightedFlowSum, flows,
                            startDateIncl, endDateIncl
                    ));
        }

        BigDecimal gain = adjEndValueIncl.subtract(adjStartValueExcl).subtract(flowSum);
        BigDecimal averageCapital = adjStartValueExcl.add(weightedFlowSum);

        return gain.divide(averageCapital, calcScale, roundingMode);
    }
}
