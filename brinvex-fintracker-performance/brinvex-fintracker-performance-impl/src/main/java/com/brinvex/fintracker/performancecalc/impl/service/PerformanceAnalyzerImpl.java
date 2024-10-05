package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.exception.NotYetImplementedException;
import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TwrCalculator;
import com.brinvex.util.java.LimitedLinkedMap;
import com.brinvex.util.java.Num;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeGrowthFactor;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeReturn;
import static com.brinvex.util.java.CollectionUtil.rangeSafeHeadMap;
import static com.brinvex.util.java.CollectionUtil.rangeSafeTailMap;
import static com.brinvex.util.java.DateUtil.minDate;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptySortedMap;

@SuppressWarnings("DuplicatedCode")
public class PerformanceAnalyzerImpl implements PerformanceAnalyzer {

    private final Function<Class<? extends TwrCalculator>, TwrCalculator> twrCalculatorProvider;

    private final Function<Class<? extends MwrCalculator>, MwrCalculator> mwrCalculatorProvider;

    public PerformanceAnalyzerImpl(
            Function<Class<? extends TwrCalculator>, TwrCalculator> twrCalculatorProvider,
            Function<Class<? extends MwrCalculator>, MwrCalculator> mwrCalculatorProvider
    ) {
        this.twrCalculatorProvider = twrCalculatorProvider;
        this.mwrCalculatorProvider = mwrCalculatorProvider;
    }

    @Override
    public List<PerfAnalysis> analyzePerformance(PerfAnalysisRequest perfAnalysisReq) {
        PeriodUnit periodUnit = perfAnalysisReq.resultPeriodUnit();
        if (periodUnit == PeriodUnit.DAY) {
            throw new IllegalArgumentException("resultPeriodUnit must not be DAY");
        }

        LocalDate startDateIncl = perfAnalysisReq.startDateIncl();
        LocalDate endDateIncl = perfAnalysisReq.endDateIncl();
        FlowTiming flowTiming = perfAnalysisReq.flowTiming();
        boolean resultRatesInPct = perfAnalysisReq.resultRatesInPercent();
        int calcScale = perfAnalysisReq.calcScale();
        int resultScale = perfAnalysisReq.resultScale();
        RoundingMode roundingMode = perfAnalysisReq.roundingMode();
        TwrCalculator twrCalculator = twrCalculatorProvider.apply(perfAnalysisReq.twrCalculatorType());
        MwrCalculator mwrCalculator = mwrCalculatorProvider.apply(perfAnalysisReq.mwrCalculatorType());
        boolean calculateTrailingAvgProfit1Y = perfAnalysisReq.calculateTrailingAvgProfit1Y();
        boolean calculateTrailingAvgFlow1Y = perfAnalysisReq.calculateTrailingAvgFlow1Y();
        boolean calculatePeriodIncome = perfAnalysisReq.calculatePeriodIncome();
        boolean calculateTrailingAvgIncome1Y = perfAnalysisReq.calculateTrailingAvgIncome1Y();
        boolean calculateTrailingTwr1Y = perfAnalysisReq.calculateTrailingTwr1Y();
        boolean calculateTrailingTwr2Y = perfAnalysisReq.calculateTrailingTwr2Y();
        boolean calculateTrailingTwr3Y = perfAnalysisReq.calculateTrailingTwr3Y();
        boolean calculateTrailingTwr5Y = perfAnalysisReq.calculateTrailingTwr5Y();
        boolean calculateTrailingTwr10Y = perfAnalysisReq.calculateTrailingTwr10Y();

        SortedMap<LocalDate, BigDecimal> flows = perfAnalysisReq.flows();
        Map<LocalDate, BigDecimal> assetValues = perfAnalysisReq.assetValues();
        SortedMap<LocalDate, BigDecimal> incomes = perfAnalysisReq.incomes();

        LocalDate startDateExcl = startDateIncl.minusDays(1);
        BigDecimal startValueExcl = assetValues.get(startDateExcl);
        Validate.notNull(startValueExcl, () -> "startAssetValueExcl must not be null, missing assetValue for startDateExcl=%s"
                .formatted(startDateExcl));

        LocalDate periodStartDateIncl = startDateIncl;
        List<PerfAnalysis> results = new ArrayList<>();
        BigDecimal cumulTwrFactor = ONE;
        BigDecimal totalContribution = startValueExcl;
        BigDecimal totalProfit = ZERO;
        SortedMap<LocalDate, BigDecimal> iterativeForwardFlows = flows;
        SortedMap<LocalDate, BigDecimal> iterativeForwardIncomes = incomes;
        int periodFrequencyPerYear = periodUnit.frequencyPerYear();
        int periodFrequencyPerYears2 = periodFrequencyPerYear * 2;
        int periodFrequencyPerYears3 = periodFrequencyPerYear * 3;
        int periodFrequencyPerYears5 = periodFrequencyPerYear * 5;
        int periodFrequencyPerYears10 = periodFrequencyPerYear * 10;
        LimitedLinkedMap<LocalDate, BigDecimal> trailingProfits1Y = calculateTrailingAvgProfit1Y ? new LimitedLinkedMap<>(periodFrequencyPerYear) : null;
        LimitedLinkedMap<LocalDate, BigDecimal> trailingFlows1Y = calculateTrailingAvgFlow1Y ? new LimitedLinkedMap<>(periodFrequencyPerYear) : null;
        LimitedLinkedMap<LocalDate, BigDecimal> trailingIncomes1Y = calculateTrailingAvgIncome1Y ? new LimitedLinkedMap<>(periodFrequencyPerYear) : null;
        LimitedLinkedMap<LocalDate, BigDecimal> trailingTwrFactors1Y = new LimitedLinkedMap<>(periodFrequencyPerYear);
        LimitedLinkedMap<LocalDate, BigDecimal> trailingTwrFactors2Y = new LimitedLinkedMap<>(periodFrequencyPerYears2);
        LimitedLinkedMap<LocalDate, BigDecimal> trailingTwrFactors3Y = new LimitedLinkedMap<>(periodFrequencyPerYears3);
        LimitedLinkedMap<LocalDate, BigDecimal> trailingTwrFactors5Y = new LimitedLinkedMap<>(periodFrequencyPerYears5);
        LimitedLinkedMap<LocalDate, BigDecimal> trailingTwrFactors10Y = new LimitedLinkedMap<>(periodFrequencyPerYears10);

        while (!periodStartDateIncl.isAfter(endDateIncl)) {
            LocalDate periodStartDateExcl = periodStartDateIncl.minusDays(1);
            LocalDate periodEndDateIncl = minDate(periodUnit.adjEndDateIncl(periodStartDateIncl), endDateIncl);
            LocalDate periodEndDateExcl = periodEndDateIncl.plusDays(1);
            BigDecimal periodStartValueExcl = assetValues.get(periodStartDateExcl);
            Validate.notNull(periodStartValueExcl, () -> "periodStartValueExcl must not be null, missing assetValue for periodStartDateExcl=%s"
                    .formatted(periodStartDateExcl));
            BigDecimal periodEndValueIncl = assetValues.get(periodEndDateIncl);
            Validate.notNull(periodEndValueIncl, () -> "periodEndValueIncl must not be null, missing assetValue for periodEndDateIncl=%s"
                    .formatted(periodEndDateIncl));

            SortedMap<LocalDate, BigDecimal> periodFlows = rangeSafeHeadMap(iterativeForwardFlows, periodEndDateExcl);

            PerfCalcRequest periodPerfCalcReq = PerfCalcRequest.builder()
                    .startDateIncl(periodStartDateIncl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(periodStartValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(periodFlows)
                    .assetValues(assetValues)
                    .flowTiming(flowTiming)
                    .annualization(DO_NOT_ANNUALIZE)
                    .calcScale(calcScale)
                    .resultScale(calcScale)
                    .roundingMode(roundingMode)
                    .build();

            BigDecimal periodTwr = twrCalculator.calculateReturn(periodPerfCalcReq);
            BigDecimal periodTwrFactor = periodTwr.add(ONE);
            cumulTwrFactor = cumulTwrFactor.multiply(periodTwrFactor).setScale(calcScale, roundingMode);
            BigDecimal annTwrFactor = annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, cumulTwrFactor, startDateIncl, periodEndDateIncl);

            BigDecimal periodMwr = null;
            if (perfAnalysisReq.calculatePeriodMwr()) {
                if (periodFlows.isEmpty()) {
                    periodMwr = periodTwr;
                } else {
                    periodMwr = mwrCalculator.calculateReturn(periodPerfCalcReq);
                }
            }
            BigDecimal cumulMwr = null;
            BigDecimal annMwr = null;
            if (perfAnalysisReq.calculateMwr()) {
                cumulMwr = mwrCalculator.calculateReturn(PerfCalcRequest.builder()
                        .startDateIncl(startDateIncl)
                        .endDateIncl(periodEndDateIncl)
                        .startAssetValueExcl(startValueExcl)
                        .endAssetValueIncl(periodEndValueIncl)
                        .flows(flows)
                        .assetValues(assetValues)
                        .flowTiming(flowTiming)
                        .annualization(DO_NOT_ANNUALIZE)
                        .calcScale(calcScale)
                        .resultScale(calcScale)
                        .roundingMode(roundingMode)
                        .build());
                annMwr = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, cumulMwr, startDateIncl, periodEndDateIncl);
            }

            BigDecimal periodFlowSum = periodFlows.values().stream().reduce(ZERO, BigDecimal::add);
            totalContribution = totalContribution.add(periodFlowSum);
            BigDecimal periodProfit = periodEndValueIncl.subtract(periodStartValueExcl).subtract(periodFlowSum);
            totalProfit = totalProfit.add(periodProfit);

            BigDecimal trailingAvgProfit1Y = null;
            if (calculateTrailingAvgProfit1Y) {
                trailingProfits1Y.put(periodStartDateIncl, periodProfit);
                trailingAvgProfit1Y = Num.avg(trailingProfits1Y.values(), resultScale, roundingMode);
            }

            BigDecimal trailingAvgFlow1Y = null;
            if (calculateTrailingAvgFlow1Y) {
                trailingFlows1Y.put(periodStartDateIncl, periodFlowSum);
                trailingAvgFlow1Y = Num.avg(trailingFlows1Y.values(), resultScale, roundingMode);
            }

            BigDecimal periodIncomeSum = null;
            BigDecimal trailingAvgIncome1Y = null;
            if (calculatePeriodIncome || calculateTrailingAvgIncome1Y) {
                SortedMap<LocalDate, BigDecimal> periodIncomes = rangeSafeHeadMap(iterativeForwardIncomes, periodEndDateExcl);
                periodIncomeSum = periodIncomes.values().stream().reduce(ZERO, BigDecimal::add);
                if (calculateTrailingAvgIncome1Y) {
                    trailingIncomes1Y.put(periodStartDateIncl, periodIncomeSum);
                    trailingAvgIncome1Y = Num.avg(trailingIncomes1Y.values(), resultScale, roundingMode);
                }
            }

            BigDecimal trailTwrFactor1Y = null;
            BigDecimal trailTwrFactor2Y = null;
            BigDecimal trailTwrFactor3Y = null;
            BigDecimal trailTwrFactor5Y = null;
            BigDecimal trailTwrFactor10Y = null;
            if (calculateTrailingTwr1Y || calculateTrailingTwr2Y || calculateTrailingTwr3Y || calculateTrailingTwr5Y || calculateTrailingTwr10Y) {
                trailingTwrFactors1Y.put(periodStartDateIncl, periodTwrFactor);
                if (trailingTwrFactors1Y.size() >= periodFrequencyPerYear) {
                    trailTwrFactor1Y = trailingTwrFactors1Y
                            .values()
                            .stream()
                            .reduce(ONE, BigDecimal::multiply)
                            .setScale(calcScale, roundingMode);
                }
                if (calculateTrailingTwr2Y || calculateTrailingTwr3Y || calculateTrailingTwr5Y || calculateTrailingTwr10Y) {
                    trailingTwrFactors2Y.put(periodStartDateIncl, periodTwrFactor);
                    if (trailingTwrFactors2Y.size() >= periodFrequencyPerYears2) {
                        assert trailTwrFactor1Y != null;
                        trailTwrFactor2Y = trailTwrFactor1Y.multiply(trailingTwrFactors2Y
                                        .reversed()
                                        .values()
                                        .stream()
                                        .skip(periodFrequencyPerYear)
                                        .reduce(ONE, BigDecimal::multiply))
                                .setScale(calcScale, roundingMode);
                    }
                    if (calculateTrailingTwr3Y || calculateTrailingTwr5Y || calculateTrailingTwr10Y) {
                        trailingTwrFactors3Y.put(periodStartDateIncl, periodTwrFactor);
                        if (trailingTwrFactors3Y.size() >= periodFrequencyPerYears3) {
                            assert trailTwrFactor2Y != null;
                            trailTwrFactor3Y = trailTwrFactor2Y.multiply(trailingTwrFactors3Y
                                            .reversed()
                                            .values()
                                            .stream()
                                            .skip(periodFrequencyPerYears2)
                                            .reduce(ONE, BigDecimal::multiply))
                                    .setScale(calcScale, roundingMode);
                        }
                        if (calculateTrailingTwr5Y || calculateTrailingTwr10Y) {
                            trailingTwrFactors5Y.put(periodStartDateIncl, periodTwrFactor);
                            if (trailingTwrFactors5Y.size() >= periodFrequencyPerYears5) {
                                assert trailTwrFactor3Y != null;
                                trailTwrFactor5Y = trailTwrFactor3Y.multiply(trailingTwrFactors5Y
                                                .reversed()
                                                .values()
                                                .stream()
                                                .skip(periodFrequencyPerYears3)
                                                .reduce(ONE, BigDecimal::multiply))
                                        .setScale(calcScale, roundingMode);
                            }
                            if (calculateTrailingTwr10Y) {
                                trailingTwrFactors10Y.put(periodStartDateIncl, periodTwrFactor);
                                if (trailingTwrFactors10Y.size() >= periodFrequencyPerYears10) {
                                    assert trailTwrFactor5Y != null;
                                    trailTwrFactor10Y = trailTwrFactor5Y.multiply(trailingTwrFactors10Y
                                                    .reversed()
                                                    .values()
                                                    .stream()
                                                    .skip(periodFrequencyPerYears5)
                                                    .reduce(ONE, BigDecimal::multiply))
                                            .setScale(calcScale, roundingMode);
                                }
                                trailTwrFactor10Y = trailTwrFactor10Y == null ? null : annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, trailTwrFactor10Y, 10);
                            }
                            trailTwrFactor5Y = trailTwrFactor5Y == null ? null : annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, trailTwrFactor5Y, 5);
                        }
                        trailTwrFactor3Y = trailTwrFactor3Y == null ? null : annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, trailTwrFactor3Y, 3);
                    }
                    trailTwrFactor2Y = trailTwrFactor2Y == null ? null : annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, trailTwrFactor2Y, 2);
                }
            }

            results.add(PerfAnalysis.builder()
                    .periodStartDateIncl(periodStartDateIncl)
                    .periodEndDateIncl(periodEndDateIncl)
                    .periodCaption(periodUnit.caption(periodStartDateIncl))
                    .periodStartAssetValueExcl(periodStartValueExcl)
                    .periodEndAssetValueIncl(periodEndValueIncl)
                    .periodFlow(periodFlowSum)
                    .periodTwr(toPctAndScale(periodTwr, resultRatesInPct, resultScale, roundingMode))
                    .cumulativeTwr(toPctAndScale(cumulTwrFactor.subtract(ONE), resultRatesInPct, resultScale, roundingMode))
                    .annualizedTwr(toPctAndScale(annTwrFactor.subtract(ONE), resultRatesInPct, resultScale, roundingMode))
                    .periodMwr(toPctAndScale(periodMwr, resultRatesInPct, resultScale, roundingMode))
                    .cumulativeMwr(toPctAndScale(cumulMwr, resultRatesInPct, resultScale, roundingMode))
                    .annualizedMwr(toPctAndScale(annMwr, resultRatesInPct, resultScale, roundingMode))
                    .totalContribution(totalContribution)
                    .periodProfit(periodProfit)
                    .totalProfit(totalProfit)
                    .trailingAvgProfit1Y(trailingAvgProfit1Y)
                    .trailingAvgFlow1Y(trailingAvgFlow1Y)
                    .periodIncome(periodIncomeSum)
                    .trailingAvgIncome1Y(trailingAvgIncome1Y)
                    .trailingTwr1Y(toPctAndScale(trailTwrFactor1Y == null ? null : trailTwrFactor1Y.subtract(ONE), resultRatesInPct, resultScale, roundingMode))
                    .trailingTwr2Y(toPctAndScale(trailTwrFactor2Y == null ? null : trailTwrFactor2Y.subtract(ONE), resultRatesInPct, resultScale, roundingMode))
                    .trailingTwr3Y(toPctAndScale(trailTwrFactor3Y == null ? null : trailTwrFactor3Y.subtract(ONE), resultRatesInPct, resultScale, roundingMode))
                    .trailingTwr5Y(toPctAndScale(trailTwrFactor5Y == null ? null : trailTwrFactor5Y.subtract(ONE), resultRatesInPct, resultScale, roundingMode))
                    .trailingTwr10Y(toPctAndScale(trailTwrFactor10Y == null ? null : trailTwrFactor10Y.subtract(ONE), resultRatesInPct, resultScale, roundingMode))
                    .build());

            periodStartDateIncl = periodEndDateIncl.plusDays(1);
            iterativeForwardFlows = rangeSafeTailMap(iterativeForwardFlows, periodEndDateExcl);
            if (calculatePeriodIncome || calculateTrailingAvgIncome1Y) {
                iterativeForwardIncomes = rangeSafeTailMap(iterativeForwardIncomes, periodEndDateExcl);
            }
        }
        return results;
    }

    private static BigDecimal toPctAndScale(BigDecimal input, boolean toPercent, int scale, RoundingMode roundingMode) {
        if (input == null) {
            return null;
        }
        if (toPercent) {
            input = input.multiply(Num._100);
        }
        return input.setScale(scale, roundingMode);
    }
}
