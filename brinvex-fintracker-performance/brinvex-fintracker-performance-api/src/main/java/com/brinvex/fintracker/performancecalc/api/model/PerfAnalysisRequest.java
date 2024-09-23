package com.brinvex.fintracker.performancecalc.api.model;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TwrCalculator;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Collection;

import static java.util.Collections.emptyList;

public record PerfAnalysisRequest(
        PeriodUnit resultPeriodUnit,
        LocalDate startDateIncl,
        LocalDate endDateIncl,
        Collection<DateAmount> cashFlows,
        Collection<DateAmount> assetValues,
        FlowTiming flowTiming,
        Class<? extends TwrCalculator> twrCalculatorType,
        Class<? extends MwrCalculator> mwrCalculatorType
) {

    @Builder(toBuilder = true)
    public PerfAnalysisRequest {
        if (startDateIncl == null) {
            throw new IllegalArgumentException("startDateIncl must not be null");
        }
        if (endDateIncl == null) {
            throw new IllegalArgumentException("endDateIncl must not be null");
        }
        if (startDateIncl.isAfter(endDateIncl)) {
            throw new IllegalArgumentException("startDateIncl must be before endDateIncl, given: %s, %s"
                    .formatted(startDateIncl, endDateIncl));
        }
        if (resultPeriodUnit == null) {
            resultPeriodUnit = PeriodUnit.MONTH;
        }
        if (cashFlows == null) {
            cashFlows = emptyList();
        }
        if (assetValues == null) {
            assetValues = emptyList();
        }
        if (twrCalculatorType == null) {
            twrCalculatorType = PerformanceCalculator.TrueTwrCalculator.class;
        }
        if (mwrCalculatorType == null) {
            mwrCalculatorType = PerformanceCalculator.ModifiedDietzMwrCalculator.class;
        }
        if (flowTiming == null) {
            flowTiming = FlowTiming.BEGINNING_OF_DAY;
        }
    }
}
