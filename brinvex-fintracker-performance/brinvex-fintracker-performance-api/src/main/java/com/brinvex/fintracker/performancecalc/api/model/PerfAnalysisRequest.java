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
import java.util.function.BiFunction;
import java.util.function.Function;

@Getter
@Accessors(fluent = true)
public final class PerfAnalysisRequest {
    private final PeriodUnit resultPeriodUnit;
    private final LocalDate analysisStartDateIncl;
    private final LocalDate analysisEndDateIncl;
    private final LocalDate investmentStartDateIncl;
    private final LocalDate investmentEndDateIncl;
    private final Function<LocalDate, BigDecimal> assetValues;
    private final BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> flows;
    private final FlowTiming twrFlowTiming;
    private final FlowTiming mwrFlowTiming;
    private final Class<? extends TwrCalculator> twrCalculatorType;
    private final Class<? extends MwrCalculator> mwrCalculatorType;
    private final int largeFlowLevelInPercent;
    private final boolean resultRatesInPercent;
    private final int calcScale;
    private final int resultRateScale;
    private final int resultAmountScale;
    private final RoundingMode roundingMode;
    private final boolean calculateMwr;
    private final boolean calculateTrailingAvgProfit1Y;
    private final boolean calculateTrailingAvgFlow1Y;
    private final boolean calculatePeriodIncome;
    private final boolean calculateTrailingAvgIncome1Y;
    private final BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> incomes;
    private final boolean calculateTrailingTwr1Y;
    private final boolean calculateTrailingTwr2Y;
    private final boolean calculateTrailingTwr3Y;
    private final boolean calculateTrailingTwr5Y;
    private final boolean calculateTrailingTwr10Y;

    @SuppressWarnings("ReplaceNullCheck")
    private PerfAnalysisRequest(
            PeriodUnit resultPeriodUnit,
            LocalDate analysisStartDateIncl,
            LocalDate analysisEndDateIncl, LocalDate investmentStartDateIncl, LocalDate investmentEndDateIncl,
            Function<LocalDate, BigDecimal> assetValuesProvider,
            Map<LocalDate, BigDecimal> assetValuesMap,
            Collection<DateAmount> assetValuesCollection,
            BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> flowsProvider,
            Map<LocalDate, BigDecimal> flowsMap,
            Collection<DateAmount> flowsCollection,
            Class<? extends TwrCalculator> twrCalculatorType,
            Class<? extends MwrCalculator> mwrCalculatorType,
            FlowTiming twrFlowTiming,
            FlowTiming mwrFlowTiming,
            Integer largeFlowLevelInPercent,
            Boolean resultRatesInPercent,
            Integer calcScale,
            Integer resultRateScale,
            Integer resultAmountScale,
            RoundingMode roundingMode,
            Boolean calculateMwr,
            Boolean calculateTrailingAvgProfit1Y,
            Boolean calculateTrailingAvgFlow1Y,
            Boolean calculatePeriodIncome,
            Boolean calculateTrailingAvgIncome1Y,
            BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> incomesProvider,
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
        this.twrFlowTiming = twrFlowTiming == null ? FlowTiming.BEGINNING_OF_DAY : twrFlowTiming;
        this.mwrFlowTiming = mwrFlowTiming == null ? FlowTiming.BEGINNING_OF_DAY : mwrFlowTiming;
        this.twrCalculatorType = twrCalculatorType == null ? TrueTwrCalculator.class : twrCalculatorType;
        this.mwrCalculatorType = mwrCalculatorType == null ? ModifiedDietzMwrCalculator.class : mwrCalculatorType;
        this.resultRatesInPercent = resultRatesInPercent != null && resultRatesInPercent;
        this.largeFlowLevelInPercent = largeFlowLevelInPercent == null ? 5 : largeFlowLevelInPercent;
        this.calcScale = calcScale == null ? 20 : calcScale;
        this.resultRateScale = resultRateScale == null ? 6 : resultRateScale;
        this.resultAmountScale = resultAmountScale == null ? 2 : resultAmountScale;
        this.roundingMode = roundingMode == null ? RoundingMode.HALF_UP : roundingMode;
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

        if (flowsProvider != null) {
            this.flows = flowsProvider;
        } else {
            this.flows = (_, _) -> PerfCalcRequest.sanitizeFlows(
                    flowsMap,
                    flowsCollection,
                    startDateIncl,
                    endDateIncl
            );
        }

        if (this.calculatePeriodIncome || this.calculateTrailingAvgIncome1Y) {
            if (incomesProvider == null && incomesMap == null && incomesCollection == null) {
                throw new IllegalArgumentException((
                        "if calculatePeriodIncome or calculateTrailingAvgIncome1Y is true, then incomes must not be null, given: %s, %s")
                        .formatted(this.calculatePeriodIncome, this.calculateTrailingAvgIncome1Y)
                );
            }
            if (incomesProvider != null) {
                this.incomes = incomesProvider;
            } else {
                this.incomes = (_, _) -> PerfCalcRequest.sanitizeFlows(
                        incomesMap,
                        incomesCollection,
                        startDateIncl,
                        endDateIncl
                );
            }
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
        private BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> flowsProvider;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> flowsMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> flowsCollection;
        @Setter(AccessLevel.NONE)
        private BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> incomesProvider;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> incomesMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> incomesCollection;
        private FlowTiming twrFlowTiming;
        private FlowTiming mwrFlowTiming;
        private Class<? extends TwrCalculator> twrCalculatorType;
        private Class<? extends MwrCalculator> mwrCalculatorType;
        private Integer largeFlowLevelInPercent;
        private Boolean resultRatesInPercent;
        private Integer calcScale;
        private Integer resultRateScale;
        private Integer resultAmountScale;
        private RoundingMode roundingMode;
        private Boolean calculateMwr;
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

        public PerfAnalysisRequestBuilder flowTiming(FlowTiming flowTiming) {
            this.twrFlowTiming = flowTiming;
            this.mwrFlowTiming = flowTiming;
            return this;
        }

        public PerfAnalysisRequestBuilder resultScale(int scale) {
            this.resultRateScale = scale;
            this.resultAmountScale = scale;
            return this;
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
        public PerfAnalysisRequestBuilder flows(BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> flows) {
            this.flowsProvider = flows;
            this.flowsMap = null;
            this.flowsCollection = null;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder flows(Map<LocalDate, BigDecimal> flows) {
            this.flowsProvider = null;
            this.flowsMap = flows;
            this.flowsCollection = null;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder flows(Collection<DateAmount> flows) {
            this.flowsProvider = null;
            this.flowsMap = null;
            this.flowsCollection = flows;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder incomes(BiFunction<LocalDate, LocalDate, SortedMap<LocalDate, BigDecimal>> flows) {
            this.incomesProvider = flows;
            this.incomesMap = null;
            this.incomesCollection = null;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder incomes(Map<LocalDate, BigDecimal> incomes) {
            this.incomesProvider = null;
            this.incomesMap = incomes;
            this.incomesCollection = null;
            return this;
        }

        @Tolerate
        public PerfAnalysisRequestBuilder incomes(Collection<DateAmount> incomes) {
            this.incomesProvider = null;
            this.incomesMap = null;
            this.incomesCollection = incomes;
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
                    flowsProvider,
                    flowsMap,
                    flowsCollection,
                    twrCalculatorType,
                    mwrCalculatorType,
                    twrFlowTiming,
                    mwrFlowTiming,
                    largeFlowLevelInPercent,
                    resultRatesInPercent,
                    calcScale,
                    resultRateScale,
                    resultAmountScale,
                    roundingMode,
                    calculateMwr,
                    calculateTrailingAvgProfit1Y,
                    calculateTrailingAvgFlow1Y,
                    calculatePeriodIncome,
                    calculateTrailingAvgIncome1Y,
                    incomesProvider,
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
