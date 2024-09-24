package com.brinvex.fintracker.performancecalc.api.model;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.ModifiedDietzMwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TrueTwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TwrCalculator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSortedMap;

@Getter
@Accessors(fluent = true)
public final class PerfAnalysisRequest {
    private final PeriodUnit resultPeriodUnit;
    private final LocalDate startDateIncl;
    private final LocalDate endDateIncl;
    private final Map<LocalDate, BigDecimal> assetValues;
    private final SortedMap<LocalDate, BigDecimal> flows;
    private final FlowTiming flowTiming;
    private final Class<? extends TwrCalculator> twrCalculatorType;
    private final Class<? extends MwrCalculator> mwrCalculatorType;
    private final Integer calcScale;
    private final Integer resultScale;
    private final RoundingMode roundingMode;

    private PerfAnalysisRequest(
            PeriodUnit resultPeriodUnit,
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            Map<LocalDate, BigDecimal> assetValuesMap,
            Collection<DateAmount> assetValuesCollection,
            Map<LocalDate, BigDecimal> flowsMap,
            Collection<DateAmount> flowsCollection,
            Class<? extends TwrCalculator> twrCalculatorType,
            Class<? extends MwrCalculator> mwrCalculatorType,
            FlowTiming flowTiming,
            Integer calcScale,
            Integer resultScale,
            RoundingMode roundingMode
    ) {
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
        this.startDateIncl = startDateIncl;
        this.endDateIncl = endDateIncl;
        this.resultPeriodUnit = resultPeriodUnit == null ? PeriodUnit.MONTH : resultPeriodUnit;
        this.flowTiming = flowTiming == null ? FlowTiming.BEGINNING_OF_DAY : flowTiming;
        this.twrCalculatorType = twrCalculatorType == null ? TrueTwrCalculator.class : twrCalculatorType;
        this.mwrCalculatorType = mwrCalculatorType == null ? ModifiedDietzMwrCalculator.class : mwrCalculatorType;
        this.calcScale = calcScale == null ? 20 : calcScale;
        this.resultScale = resultScale == null ? 6 : resultScale;
        this.roundingMode = roundingMode == null ? RoundingMode.HALF_UP : roundingMode;

        this.assetValues = unmodifiableMap(PerfCalcRequest.sanitizeAssetValues(
                assetValuesMap,
                assetValuesCollection,
                startDateIncl,
                endDateIncl));

        this.flows = unmodifiableSortedMap(PerfCalcRequest.sanitizeFlows(
                flowsMap,
                flowsCollection,
                startDateIncl,
                endDateIncl));
    }

    public static PerfAnalysisRequestBuilder builder() {
        return new PerfAnalysisRequestBuilder();
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class PerfAnalysisRequestBuilder {
        private PeriodUnit resultPeriodUnit;
        private LocalDate startDateIncl;
        private LocalDate endDateIncl;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> assetValuesMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> assetValuesCollection;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> flowsMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> flowsCollection;
        private FlowTiming flowTiming;
        private Class<? extends TwrCalculator> twrCalculatorType;
        private Class<? extends MwrCalculator> mwrCalculatorType;
        private Integer calcScale;
        private Integer resultScale;
        private RoundingMode roundingMode;

        private PerfAnalysisRequestBuilder() {
        }

        @Tolerate
        public PerfAnalysisRequestBuilder assetValues(Map<LocalDate, BigDecimal> assetValues) {
            this.assetValuesCollection = null;
            this.assetValuesMap = assetValues;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder assetValues(Collection<DateAmount> assetValues) {
            this.assetValuesMap = null;
            this.assetValuesCollection = assetValues;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder flows(Map<LocalDate, BigDecimal> flows) {
            this.flowsCollection = null;
            this.flowsMap = flows;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder flows(Collection<DateAmount> flows) {
            this.flowsMap = null;
            this.flowsCollection = flows;
            return this;
        }

        public PerfAnalysisRequest build() {
            return new PerfAnalysisRequest(
                    this.resultPeriodUnit,
                    this.startDateIncl,
                    this.endDateIncl,
                    this.assetValuesMap,
                    this.assetValuesCollection,
                    this.flowsMap,
                    this.flowsCollection,
                    this.twrCalculatorType,
                    this.mwrCalculatorType,
                    this.flowTiming,
                    this.calcScale,
                    this.resultScale,
                    this.roundingMode
            );
        }

    }
}
