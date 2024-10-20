package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.CollectionUtil;
import com.brinvex.util.java.Num;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map.Entry;
import java.util.SortedMap;

import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeReturn;
import static java.math.BigDecimal.ZERO;

public abstract class BaseCalculatorImpl implements PerformanceCalculator {

    @Override
    public final BigDecimal calculateReturn(PerfCalcRequest perfCalcRequest) {

        SortedMap<LocalDate, BigDecimal> flows = perfCalcRequest.flows();
        BigDecimal startValueExcl = perfCalcRequest.startAssetValueExcl();
        BigDecimal endValueIncl = perfCalcRequest.endAssetValueIncl();
        if (!flows.isEmpty()) {
            FlowTiming flowTiming = perfCalcRequest.flowTiming();
            switch (flowTiming) {
                case BEGINNING_OF_DAY -> {
                    Entry<LocalDate, BigDecimal> firstFlowEntry = flows.firstEntry();
                    LocalDate firstFlowDate = firstFlowEntry.getKey();
                    if (firstFlowDate.isEqual(perfCalcRequest.startDateIncl())) {
                        startValueExcl = startValueExcl.add(firstFlowEntry.getValue());
                        flows = CollectionUtil.rangeSafeTailMap(flows, firstFlowDate.plusDays(1));
                    }
                }
                case END_OF_DAY -> {
                    Entry<LocalDate, BigDecimal> lastFlowEntry = flows.lastEntry();
                    LocalDate lastFlowDate = lastFlowEntry.getKey();
                    if (lastFlowDate.isEqual(perfCalcRequest.endDateIncl())) {
                        endValueIncl = endValueIncl.subtract(lastFlowEntry.getValue());
                        flows = CollectionUtil.rangeSafeHeadMap(flows, lastFlowDate);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + flowTiming);
            }
        }

        if (startValueExcl.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("startValueExcl must be greater than zero; given: %s".formatted(startValueExcl));
        }

        BigDecimal cumulReturn;
        if (flows.isEmpty()) {
            cumulReturn = SimpleReturnCalculatorImpl.calculateSimpleCumulReturn(
                    startValueExcl,
                    endValueIncl,
                    perfCalcRequest.calcScale(),
                    perfCalcRequest.roundingMode()
            );
        } else {
            PerfCalcRequest adjPerfCalcRequest = perfCalcRequest.toBuilder()
                    .startAssetValueExcl(startValueExcl)
                    .endAssetValueIncl(endValueIncl)
                    .flows(flows)
                    .build();
            cumulReturn = calculateCumulativeReturn(adjPerfCalcRequest);
        }

        BigDecimal unscaledAnnReturn = annualizeReturn(
                perfCalcRequest.annualization(),
                cumulReturn,
                perfCalcRequest.startDateIncl(),
                perfCalcRequest.endDateIncl()
        );
        if (perfCalcRequest.resultInPercent()) {
            unscaledAnnReturn = unscaledAnnReturn.multiply(Num._100);
        }
        return unscaledAnnReturn.setScale(perfCalcRequest.resultScale(), perfCalcRequest.roundingMode());
    }

    protected abstract BigDecimal calculateCumulativeReturn(PerfCalcRequest calcRequest);
}
