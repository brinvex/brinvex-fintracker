package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TwrCalculator;
import com.brinvex.util.java.CollectionUtil;
import com.brinvex.util.java.LimitedLinkedMap;
import com.brinvex.util.java.Num;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SortedMap;
import java.util.function.Function;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeGrowthFactor;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeReturn;
import static com.brinvex.util.java.CollectionUtil.rangeSafeHeadMap;
import static com.brinvex.util.java.CollectionUtil.rangeSafeTailMap;
import static com.brinvex.util.java.DateUtil.maxDate;
import static com.brinvex.util.java.DateUtil.minDate;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

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

    @SuppressWarnings("DataFlowIssue")
    @Override
    public SequencedCollection<PerfAnalysis> analyzePerformance(PerfAnalysisRequest req) {
        PeriodUnit periodUnit = req.resultPeriodUnit();
        LocalDate analysisStartDateIncl = req.analysisStartDateIncl();
        LocalDate analysisEndDateIncl = req.analysisEndDateIncl();
        FlowTiming flowTiming = req.flowTiming();
        boolean resultRatesInPct = req.resultRatesInPercent();
        int calcScale = req.calcScale();
        int resultScale = req.resultScale();
        RoundingMode roundingMode = req.roundingMode();
        TwrCalculator twrCalculator = twrCalculatorProvider.apply(req.twrCalculatorType());
        MwrCalculator mwrCalculator = mwrCalculatorProvider.apply(req.mwrCalculatorType());
        boolean calculateTrailingAvgProfit1Y = req.calculateTrailingAvgProfit1Y();
        boolean calculateTrailingAvgFlow1Y = req.calculateTrailingAvgFlow1Y();
        boolean calculatePeriodIncome = req.calculatePeriodIncome();
        boolean calculateTrailingAvgIncome1Y = req.calculateTrailingAvgIncome1Y();
        boolean calculateTrailingTwr1Y = req.calculateTrailingTwr1Y();
        boolean calculateTrailingTwr2Y = req.calculateTrailingTwr2Y();
        boolean calculateTrailingTwr3Y = req.calculateTrailingTwr3Y();
        boolean calculateTrailingTwr5Y = req.calculateTrailingTwr5Y();
        boolean calculateTrailingTwr10Y = req.calculateTrailingTwr10Y();
        SortedMap<LocalDate, BigDecimal> flows = req.flows();
        Function<LocalDate, BigDecimal> assetValues = req.assetValues();
        SortedMap<LocalDate, BigDecimal> incomes = req.incomes();

        LocalDate calcStartDateIncl = maxDate(analysisStartDateIncl, req.investmentStartDateIncl());
        LocalDate calcStartDateExcl = calcStartDateIncl.minusDays(1);
        LocalDate calcEndDateIncl = minDate(analysisEndDateIncl, req.investmentEndDateIncl());
        LocalDate calcEndDateExcl = calcEndDateIncl.plusDays(1);

        SequencedMap<String, PerfAnalysis> results = new LinkedHashMap<>();

        {
            LocalDate periodStartDateIncl = analysisStartDateIncl;
            while (periodStartDateIncl.isBefore(calcStartDateIncl)) {
                LocalDate periodEndDateIncl = minDate(periodUnit.adjEndDateIncl(periodStartDateIncl), calcStartDateIncl);
                String periodCaption = periodUnit.caption(periodStartDateIncl);
                results.put(periodCaption, PerfAnalysis.builder()
                        .periodStartDateIncl(periodStartDateIncl)
                        .periodEndDateIncl(periodEndDateIncl)
                        .periodCaption(periodCaption)
                        .build());
                //For the next iteration
                periodStartDateIncl = periodEndDateIncl.plusDays(1);
            }
        }
        if (!calcStartDateIncl.isAfter(calcEndDateIncl)) {
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

            BigDecimal startValueExcl = assetValues.apply(calcStartDateExcl);
            Validate.notNull(startValueExcl, () -> "startValueExcl must not be null, missing assetValue for calcStartDateExcl=%s"
                    .formatted(calcStartDateExcl));
            BigDecimal cumulTwrFactor = ONE;
            BigDecimal totalContribution = startValueExcl;
            BigDecimal totalProfit = ZERO;

            LocalDate periodStartDateIncl = calcStartDateIncl;
            while (!periodStartDateIncl.isAfter(calcEndDateIncl)) {
                LocalDate periodStartDateExcl = periodStartDateIncl.minusDays(1);
                LocalDate periodEndDateIncl = minDate(periodUnit.adjEndDateIncl(periodStartDateIncl), calcEndDateIncl);
                LocalDate periodEndDateExcl = periodEndDateIncl.plusDays(1);
                BigDecimal periodStartValueExcl = periodStartDateIncl.isEqual(calcStartDateIncl) ? startValueExcl : assetValues.apply(periodStartDateExcl);
                Validate.notNull(periodStartValueExcl, () -> "periodStartValueExcl must not be null, missing assetValue for periodStartDateExcl=%s"
                        .formatted(periodStartDateExcl));
                BigDecimal periodEndValueIncl = assetValues.apply(periodEndDateIncl);
                Validate.notNull(periodEndValueIncl, () -> "periodEndValueIncl must not be null, missing assetValue for periodEndDateIncl=%s"
                        .formatted(periodEndDateIncl));

                SortedMap<LocalDate, BigDecimal> periodFlows = rangeSafeHeadMap(iterativeForwardFlows, periodEndDateExcl);

                BigDecimal periodTwr;
                PerfCalcRequest periodPerfCalcReq;

                BigDecimal adjPeriodStartValueExcl = periodStartValueExcl;
                SortedMap<LocalDate, BigDecimal> adjPeriodFlows = periodFlows;
                if (!periodFlows.isEmpty()) {
                    if (flowTiming == BEGINNING_OF_DAY) {
                        Map.Entry<LocalDate, BigDecimal> firstFlowEntry = periodFlows.firstEntry();
                        LocalDate firstFlowDate = firstFlowEntry.getKey();
                        if (firstFlowDate.isEqual(periodStartDateIncl)) {
                            adjPeriodStartValueExcl = periodStartValueExcl.add(firstFlowEntry.getValue());
                            adjPeriodFlows = CollectionUtil.rangeSafeTailMap(flows, firstFlowDate.plusDays(1));
                        }
                    }
                }
                if (adjPeriodStartValueExcl.compareTo(ZERO) == 0) {
                    if (adjPeriodFlows.isEmpty()) {
                        if (periodEndValueIncl.compareTo(ZERO) == 0) {
                            periodTwr = ZERO;
                        } else {
                            throw new IllegalArgumentException("if periodStartValueExcl is zero and periodFlows is empty, then periodEndValueIncl must be zero; given: %s"
                                    .formatted(periodEndValueIncl));
                        }
                        periodPerfCalcReq = null;
                    } else {
                        LocalDate adjPeriodStartDateIncl;
                        adjPeriodStartDateIncl = switch (flowTiming) {
                            case BEGINNING_OF_DAY -> adjPeriodFlows.firstKey();
                            case END_OF_DAY -> adjPeriodFlows.firstKey().plusDays(1);
                        };
                        periodPerfCalcReq = PerfCalcRequest.builder()
                                .startDateIncl(adjPeriodStartDateIncl)
                                .endDateIncl(periodEndDateIncl)
                                .startAssetValueExcl(periodStartValueExcl)
                                .endAssetValueIncl(periodEndValueIncl)
                                .flows(adjPeriodFlows)
                                .assetValues(assetValues)
                                .flowTiming(flowTiming)
                                .annualization(DO_NOT_ANNUALIZE)
                                .calcScale(calcScale)
                                .resultScale(calcScale)
                                .roundingMode(roundingMode)
                                .build();
                        periodTwr = twrCalculator.calculateReturn(periodPerfCalcReq);
                    }
                } else {
                    periodPerfCalcReq = PerfCalcRequest.builder()
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
                    periodTwr = twrCalculator.calculateReturn(periodPerfCalcReq);
                }

                BigDecimal periodTwrFactor = periodTwr.add(ONE);
                cumulTwrFactor = cumulTwrFactor.multiply(periodTwrFactor).setScale(calcScale, roundingMode);
                BigDecimal annTwrFactor = annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, cumulTwrFactor, calcStartDateIncl, periodEndDateIncl);

                BigDecimal periodMwr;
                if (req.calculatePeriodMwr()) {
                    if (adjPeriodFlows.isEmpty()) {
                        periodMwr = periodTwr;
                    } else {
                        periodMwr = mwrCalculator.calculateReturn(periodPerfCalcReq);
                    }
                } else {
                    periodMwr = null;
                }
                BigDecimal cumulMwr;
                BigDecimal annMwr;
                if (req.calculateMwr()) {
                    if (startValueExcl.compareTo(ZERO) == 0) {
                        SortedMap<LocalDate, BigDecimal> backwardFlows = rangeSafeHeadMap(flows, periodEndDateExcl);
                        if (backwardFlows.isEmpty()) {
                            cumulMwr = ZERO;
                        } else {
                            cumulMwr = mwrCalculator.calculateReturn(PerfCalcRequest.builder()
                                    .startDateIncl(backwardFlows.firstKey())
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
                        }
                    } else {
                        cumulMwr = mwrCalculator.calculateReturn(PerfCalcRequest.builder()
                                .startDateIncl(calcStartDateIncl)
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
                    }
                    annMwr = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, cumulMwr, calcStartDateIncl, periodEndDateIncl);
                } else {
                    cumulMwr = null;
                    annMwr = null;
                }

                BigDecimal periodFlowSum = periodFlows.values().stream().reduce(ZERO, BigDecimal::add);
                totalContribution = totalContribution.add(periodFlowSum);
                BigDecimal periodProfit = periodEndValueIncl.subtract(periodStartValueExcl).subtract(periodFlowSum);
                totalProfit = totalProfit.add(periodProfit);

                BigDecimal trailingAvgProfit1Y;
                if (calculateTrailingAvgProfit1Y) {
                    trailingProfits1Y.put(periodStartDateIncl, periodProfit);
                    trailingAvgProfit1Y = Num.avg(trailingProfits1Y.values(), resultScale, roundingMode);
                } else {
                    trailingAvgProfit1Y = null;
                }

                BigDecimal trailingAvgFlow1Y;
                if (calculateTrailingAvgFlow1Y) {
                    trailingFlows1Y.put(periodStartDateIncl, periodFlowSum);
                    trailingAvgFlow1Y = Num.avg(trailingFlows1Y.values(), resultScale, roundingMode);
                } else {
                    trailingAvgFlow1Y = null;
                }

                BigDecimal periodIncomeSum;
                BigDecimal trailingAvgIncome1Y;
                if (calculatePeriodIncome || calculateTrailingAvgIncome1Y) {
                    SortedMap<LocalDate, BigDecimal> periodIncomes = rangeSafeHeadMap(iterativeForwardIncomes, periodEndDateExcl);
                    periodIncomeSum = periodIncomes.values().stream().reduce(ZERO, BigDecimal::add);
                    if (calculateTrailingAvgIncome1Y) {
                        trailingIncomes1Y.put(periodStartDateIncl, periodIncomeSum);
                        trailingAvgIncome1Y = Num.avg(trailingIncomes1Y.values(), resultScale, roundingMode);
                    } else {
                        trailingAvgIncome1Y = null;
                    }
                } else {
                    periodIncomeSum = null;
                    trailingAvgIncome1Y = null;
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

                String periodCaption = periodUnit.caption(periodStartDateIncl);
                results.put(periodCaption, PerfAnalysis.builder()
                        .periodStartDateIncl(periodStartDateIncl)
                        .periodEndDateIncl(periodEndDateIncl)
                        .periodCaption(periodCaption)
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

                //For the next iteration
                {
                    periodStartDateIncl = periodEndDateExcl;
                    iterativeForwardFlows = rangeSafeTailMap(iterativeForwardFlows, periodEndDateExcl);
                    if (calculatePeriodIncome || calculateTrailingAvgIncome1Y) {
                        iterativeForwardIncomes = rangeSafeTailMap(iterativeForwardIncomes, periodEndDateExcl);
                    }
                }
            }
        }
        {
            LocalDate periodStartDateIncl = calcEndDateExcl;
            while (!periodStartDateIncl.isAfter(analysisEndDateIncl)) {
                LocalDate periodEndDateIncl = minDate(periodUnit.adjEndDateIncl(periodStartDateIncl), analysisEndDateIncl);
                String periodCaption = periodUnit.caption(periodStartDateIncl);
                results.putIfAbsent(periodCaption, PerfAnalysis.builder()
                        .periodStartDateIncl(periodStartDateIncl)
                        .periodEndDateIncl(periodEndDateIncl)
                        .periodCaption(periodCaption)
                        .build());
                //For the next iteration
                periodStartDateIncl = periodEndDateIncl.plusDays(1);
            }
        }

        return (SequencedCollection<PerfAnalysis>) results.values();
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
