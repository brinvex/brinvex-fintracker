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
import java.util.function.Function;

import static java.util.Collections.unmodifiableSortedMap;

@Getter
@Accessors(fluent = true)
public final class PerfAnalysisRequest {
    private final PeriodUnit resultPeriodUnit;
    private final LocalDate analysisStartDateIncl;
    private final LocalDate analysisEndDateIncl;
    private final LocalDate investmentStartDateIncl;
    private final LocalDate investmentEndDateIncl;
    private final Function<LocalDate, BigDecimal> assetValues;
    private final SortedMap<LocalDate, BigDecimal> flows;
    private final FlowTiming flowTiming;
    private final Class<? extends TwrCalculator> twrCalculatorType;
    private final Class<? extends MwrCalculator> mwrCalculatorType;
    private final int largeFlowLevelInPercent;
    private final boolean resultRatesInPercent;
    private final int calcScale;
    private final int resultScale;
    private final RoundingMode roundingMode;
    private final boolean calculatePeriodMwr;
    private final boolean calculateMwr;
    private final boolean calculateTrailingAvgProfit1Y;
    private final boolean calculateTrailingAvgFlow1Y;
    private final boolean calculatePeriodIncome;
    private final boolean calculateTrailingAvgIncome1Y;
    private final SortedMap<LocalDate, BigDecimal> incomes;
    private final boolean calculateTrailingTwr1Y;
    private final boolean calculateTrailingTwr2Y;
    private final boolean calculateTrailingTwr3Y;
    private final boolean calculateTrailingTwr5Y;
    private final boolean calculateTrailingTwr10Y;

    private PerfAnalysisRequest(
            PeriodUnit resultPeriodUnit,
            LocalDate analysisStartDateIncl,
            LocalDate analysisEndDateIncl, LocalDate investmentStartDateIncl, LocalDate investmentEndDateIncl,
            Function<LocalDate, BigDecimal> assetValuesProvider,
            Map<LocalDate, BigDecimal> assetValuesMap,
            Collection<DateAmount> assetValuesCollection,
            Map<LocalDate, BigDecimal> flowsMap,
            Collection<DateAmount> flowsCollection,
            Class<? extends TwrCalculator> twrCalculatorType,
            Class<? extends MwrCalculator> mwrCalculatorType,
            FlowTiming flowTiming,
            Integer largeFlowLevelInPercent,
            Boolean resultRatesInPercent,
            Integer calcScale,
            Integer resultScale,
            RoundingMode roundingMode,
            Boolean calculateMwr, Boolean calculatePeriodMwr,
            Boolean calculateTrailingAvgProfit1Y,
            Boolean calculateTrailingAvgFlow1Y,
            Boolean calculatePeriodIncome,
            Boolean calculateTrailingAvgIncome1Y,
            Map<LocalDate, BigDecimal> incomesMap,
            Collection<DateAmount> incomesCollection,
            Boolean calculateTrailingTwr1Y,
            Boolean calculateTrailingTwr2Y,
            Boolean calculateTrailingTwr3Y,
            Boolean calculateTrailingTwr5Y,
            Boolean calculateTrailingTwr10Y
    ) {
        if (analysisStartDateIncl == null) {
            throw new IllegalArgumentException("startDateIncl must not be null");
        }
        if (analysisEndDateIncl == null) {
            throw new IllegalArgumentException("endDateIncl must not be null");
        }
        if (analysisStartDateIncl.isAfter(analysisEndDateIncl)) {
            throw new IllegalArgumentException("startDateIncl must be before endDateIncl, given: %s, %s"
                    .formatted(analysisStartDateIncl, analysisEndDateIncl));
        }
        if (resultPeriodUnit == PeriodUnit.DAY) {
            throw new IllegalArgumentException("resultPeriodUnit must not be DAY");
        }
        this.analysisStartDateIncl = analysisStartDateIncl;
        this.analysisEndDateIncl = analysisEndDateIncl;
        this.investmentStartDateIncl = investmentStartDateIncl == null ? analysisStartDateIncl : investmentStartDateIncl;
        this.investmentEndDateIncl = investmentEndDateIncl == null ? analysisEndDateIncl : investmentEndDateIncl;
        this.resultPeriodUnit = resultPeriodUnit == null ? PeriodUnit.MONTH : resultPeriodUnit;
        this.flowTiming = flowTiming == null ? FlowTiming.BEGINNING_OF_DAY : flowTiming;
        this.twrCalculatorType = twrCalculatorType == null ? TrueTwrCalculator.class : twrCalculatorType;
        this.mwrCalculatorType = mwrCalculatorType == null ? ModifiedDietzMwrCalculator.class : mwrCalculatorType;
        this.resultRatesInPercent = resultRatesInPercent != null && resultRatesInPercent;
        this.largeFlowLevelInPercent = largeFlowLevelInPercent == null ? 5 : largeFlowLevelInPercent;
        this.calcScale = calcScale == null ? 20 : calcScale;
        this.resultScale = resultScale == null ? 6 : resultScale;
        this.roundingMode = roundingMode == null ? RoundingMode.HALF_UP : roundingMode;
        this.calculatePeriodMwr = calculatePeriodMwr != null && calculatePeriodMwr;
        this.calculateMwr = calculateMwr != null && calculateMwr;
        this.calculateTrailingAvgProfit1Y = calculateTrailingAvgProfit1Y != null && calculateTrailingAvgProfit1Y;
        this.calculateTrailingAvgFlow1Y = calculateTrailingAvgFlow1Y != null && calculateTrailingAvgFlow1Y;
        this.calculatePeriodIncome = calculatePeriodIncome != null && calculatePeriodIncome;
        this.calculateTrailingAvgIncome1Y = calculateTrailingAvgIncome1Y != null && calculateTrailingAvgIncome1Y;
        this.calculateTrailingTwr1Y = calculateTrailingTwr1Y != null && calculateTrailingTwr1Y;
        this.calculateTrailingTwr2Y = calculateTrailingTwr2Y != null && calculateTrailingTwr2Y;
        this.calculateTrailingTwr3Y = calculateTrailingTwr3Y != null && calculateTrailingTwr3Y;
        this.calculateTrailingTwr5Y = calculateTrailingTwr5Y != null && calculateTrailingTwr5Y;
        this.calculateTrailingTwr10Y = calculateTrailingTwr10Y != null && calculateTrailingTwr10Y;

        LocalDate startDateIncl = this.analysisStartDateIncl.isAfter(this.investmentStartDateIncl) ? this.analysisStartDateIncl : this.investmentStartDateIncl;
        LocalDate endDateIncl = this.analysisEndDateIncl.isBefore(this.investmentEndDateIncl) ? this.analysisEndDateIncl : this.investmentEndDateIncl;

        this.assetValues = PerfCalcRequest.sanitizeAssetValues(
                assetValuesProvider,
                assetValuesMap,
                assetValuesCollection,
                startDateIncl,
                endDateIncl
        );

        this.flows = unmodifiableSortedMap(PerfCalcRequest.sanitizeFlows(
                flowsMap,
                flowsCollection,
                startDateIncl,
                endDateIncl
        ));

        if (this.calculatePeriodIncome || this.calculateTrailingAvgIncome1Y) {
            if (incomesMap == null && incomesCollection == null) {
                throw new IllegalArgumentException((
                        "if calculatePeriodIncome or calculateTrailingAvgIncome1Y is true, then incomes must not be null, given: %s, %s")
                        .formatted(this.calculatePeriodIncome, this.calculateTrailingAvgIncome1Y)
                );
            }
            this.incomes = unmodifiableSortedMap(PerfCalcRequest.sanitizeFlows(
                    incomesMap,
                    incomesCollection,
                    startDateIncl,
                    endDateIncl
            ));
        } else {
            this.incomes = null;
        }
    }

    public static PerfAnalysisRequestBuilder builder() {
        return new PerfAnalysisRequestBuilder();
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class PerfAnalysisRequestBuilder {
        private PeriodUnit resultPeriodUnit;
        private LocalDate analysisStartDateIncl;
        private LocalDate analysisEndDateIncl;
        private LocalDate investmentStartDateIncl;
        private LocalDate investmentEndDateIncl;
        @Setter(AccessLevel.NONE)
        private Function<LocalDate, BigDecimal> assetValuesProvider;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> assetValuesMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> assetValuesCollection;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> flowsMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> flowsCollection;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> incomesMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> incomesCollection;
        private FlowTiming flowTiming;
        private Class<? extends TwrCalculator> twrCalculatorType;
        private Class<? extends MwrCalculator> mwrCalculatorType;
        private Integer largeFlowLevelInPercent;
        private Boolean resultRatesInPercent;
        private Integer calcScale;
        private Integer resultScale;
        private RoundingMode roundingMode;
        private Boolean calculateMwr;
        private Boolean calculatePeriodMwr;
        private Boolean calculateTrailingAvgProfit1Y;
        private Boolean calculateTrailingAvgFlow1Y;
        private Boolean calculatePeriodIncome;
        private Boolean calculateTrailingAvgIncome1Y;
        private Boolean calculateTrailingTwr1Y;
        private Boolean calculateTrailingTwr2Y;
        private Boolean calculateTrailingTwr3Y;
        private Boolean calculateTrailingTwr5Y;
        private Boolean calculateTrailingTwr10Y;

        private PerfAnalysisRequestBuilder() {
        }

        @Tolerate
        public PerfAnalysisRequestBuilder assetValues(Function<LocalDate, BigDecimal> assetValues) {
            this.assetValuesProvider = assetValues;
            this.assetValuesMap = null;
            this.assetValuesCollection = null;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder assetValues(Map<LocalDate, BigDecimal> assetValues) {
            this.assetValuesProvider = null;
            this.assetValuesMap = assetValues;
            this.assetValuesCollection = null;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder assetValues(Collection<DateAmount> assetValues) {
            this.assetValuesProvider = null;
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

        @Tolerate
        public PerfAnalysisRequestBuilder incomes(Map<LocalDate, BigDecimal> incomesMap) {
            this.incomesMap = incomesMap;
            this.incomesCollection = null;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder incomes(Collection<DateAmount> incomesCollection) {
            this.incomesMap = null;
            this.incomesCollection = incomesCollection;
            return this;
        }

        public PerfAnalysisRequest build() {
            return new PerfAnalysisRequest(
                    resultPeriodUnit,
                    analysisStartDateIncl,
                    analysisEndDateIncl,
                    investmentStartDateIncl,
                    investmentEndDateIncl,
                    assetValuesProvider,
                    assetValuesMap,
                    assetValuesCollection,
                    flowsMap,
                    flowsCollection,
                    twrCalculatorType,
                    mwrCalculatorType,
                    flowTiming,
                    largeFlowLevelInPercent,
                    resultRatesInPercent,
                    calcScale,
                    resultScale,
                    roundingMode,
                    calculateMwr,
                    calculatePeriodMwr,
                    calculateTrailingAvgProfit1Y,
                    calculateTrailingAvgFlow1Y,
                    calculatePeriodIncome,
                    calculateTrailingAvgIncome1Y,
                    incomesMap,
                    incomesCollection,
                    calculateTrailingTwr1Y,
                    calculateTrailingTwr2Y,
                    calculateTrailingTwr3Y,
                    calculateTrailingTwr5Y,
                    calculateTrailingTwr10Y
            );
        }

    }
}
