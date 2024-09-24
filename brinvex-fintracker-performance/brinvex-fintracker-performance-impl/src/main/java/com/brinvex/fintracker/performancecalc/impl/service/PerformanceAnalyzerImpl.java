package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.LinkedModifiedDietzTwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.ModifiedDietzMwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TwrCalculator;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SortedMap;
import java.util.function.Function;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeGrowthFactor;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeReturn;
import static com.brinvex.util.java.Collectors.toTreeMap;
import static com.brinvex.util.java.DateUtil.minDate;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyMap;

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
        LocalDate startDateIncl = perfAnalysisReq.startDateIncl();
        LocalDate endDateIncl = perfAnalysisReq.endDateIncl();
        FlowTiming flowTiming = perfAnalysisReq.flowTiming();
        PeriodUnit periodUnit = perfAnalysisReq.resultPeriodUnit();
        Integer calcScale = perfAnalysisReq.calcScale();
        Integer resultScale = perfAnalysisReq.resultScale();
        RoundingMode roundingMode = perfAnalysisReq.roundingMode();
        TwrCalculator twrCalculator = twrCalculatorProvider.apply(perfAnalysisReq.twrCalculatorType());
        MwrCalculator mwrCalculator = mwrCalculatorProvider.apply(perfAnalysisReq.mwrCalculatorType());

        SequencedMap<LocalDate, BigDecimal> flows = perfAnalysisReq.flows();
        Map<LocalDate, BigDecimal> assetValues = perfAnalysisReq.assetValues();

        if (periodUnit == PeriodUnit.DAY) {
            return analyzeDailyPerformance(
                    startDateIncl,
                    endDateIncl,
                    assetValues,
                    flows,
                    twrCalculator,
                    mwrCalculator,
                    flowTiming,
                    calcScale,
                    resultScale,
                    roundingMode
            );
        }

        LocalDate startDateExcl = startDateIncl.minusDays(1);
        BigDecimal startValueExcl = assetValues.get(startDateExcl);
        Validate.notNull(startValueExcl, () -> "startAssetValueExcl must not be null, missing assetValue for startDateExcl=%s"
                .formatted(startDateExcl));

        SortedMap<LocalDate, BigDecimal> sortedFlows = flows.entrySet()
                .stream()
                .dropWhile(e -> e.getKey().isBefore(startDateIncl))
                .takeWhile(e -> !e.getKey().isAfter(endDateIncl))
                .collect(toTreeMap(Map.Entry::getKey, Map.Entry::getValue));

        LocalDate periodStartDateIncl = startDateIncl;
        List<PerfAnalysis> results = new ArrayList<>();
        BigDecimal cumulTwrFactor = ONE;
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

            SortedMap<LocalDate, BigDecimal> periodFlows = sortedFlows.subMap(periodStartDateIncl, periodEndDateExcl);

            PerfCalcRequest periodPerfCalcReq = PerfCalcRequest.builder()
                    .startDateIncl(periodStartDateExcl)
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

            BigDecimal periodMwr = mwrCalculator.calculateReturn(periodPerfCalcReq);
            BigDecimal cumulMwr = mwrCalculator.calculateReturn(PerfCalcRequest.builder()
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
            BigDecimal annMwr = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, cumulMwr, startDateIncl, periodEndDateIncl);

            BigDecimal periodTwr;
            if (periodUnit == PeriodUnit.MONTH && twrCalculator instanceof LinkedModifiedDietzTwrCalculator && mwrCalculator instanceof ModifiedDietzMwrCalculator) {
                periodTwr = periodMwr;
            } else {
                periodTwr = twrCalculator.calculateReturn(periodPerfCalcReq);
            }
            cumulTwrFactor = cumulTwrFactor.multiply(periodTwr.add(ONE)).setScale(calcScale, roundingMode);
            BigDecimal annTwrFactor = annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, cumulTwrFactor, startDateIncl, periodEndDateIncl);

            BigDecimal periodFlowSum = periodFlows.values().stream().reduce(ZERO, BigDecimal::add);

            results.add(PerfAnalysis.builder()
                    .periodStartDateIncl(periodStartDateIncl)
                    .periodEndDateIncl(periodEndDateIncl)
                    .periodStartAssetValueExcl(periodStartValueExcl)
                    .periodEndAssetValueIncl(periodEndValueIncl)
                    .flowSum(periodFlowSum)
                    .periodTwr(periodTwr.setScale(resultScale, roundingMode))
                    .cumulativeTwr(cumulTwrFactor.subtract(ONE).setScale(resultScale, roundingMode))
                    .annualizedTwr(annTwrFactor.subtract(ONE).setScale(resultScale, roundingMode))
                    .periodMwr(periodMwr.setScale(resultScale, roundingMode))
                    .cumulativeMwr(cumulMwr.setScale(resultScale, roundingMode))
                    .annualizedMwr(annMwr.setScale(resultScale, roundingMode))
                    .build());

            periodStartDateIncl = periodEndDateIncl.plusDays(1);
            sortedFlows = sortedFlows.tailMap(periodEndDateExcl);
        }
        return results;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private List<PerfAnalysis> analyzeDailyPerformance(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            Map<LocalDate, BigDecimal> assetValues,
            Map<LocalDate, BigDecimal> flows,
            TwrCalculator twrCalculator,
            MwrCalculator mwrCalculator,
            FlowTiming flowTiming,
            int calcScale,
            int resultScale,
            RoundingMode roundingMode
    ) {

        List<PerfAnalysis> results = new ArrayList<>();
        BigDecimal cumulTwrFactor = ONE;

        LocalDate startDateExcl = startDateIncl.minusDays(1);
        BigDecimal startValueExcl = assetValues.get(startDateExcl);
        Validate.notNull(startValueExcl, () -> "startAssetValueExcl must not be null, missing assetValue for startDateExcl=%s"
                .formatted(startDateExcl));

        LocalDate periodStartDateIncl = startDateIncl;
        while (!periodStartDateIncl.isAfter(endDateIncl)) {
            LocalDate periodStartDateExcl = periodStartDateIncl.minusDays(1);
            LocalDate periodEndDateIncl = periodStartDateIncl;
            BigDecimal periodStartValueExcl = assetValues.get(periodStartDateExcl);
            Validate.notNull(periodStartValueExcl, () -> "periodStartValueExcl must not be null, missing assetValue for periodStartDateExcl=%s"
                    .formatted(periodStartDateExcl));
            BigDecimal periodEndValueIncl = assetValues.get(endDateIncl);
            Validate.notNull(periodEndValueIncl, () -> "periodEndValueIncl must not be null, missing assetValue for endDateIncl=%s"
                    .formatted(periodEndDateIncl));

            Map<LocalDate, BigDecimal> periodFlows = Optional.ofNullable(flows.get(periodEndDateIncl))
                    .map(f -> Map.of(periodEndDateIncl, f))
                    .orElse(emptyMap());

            PerfCalcRequest periodTwrPerfCalcReq = PerfCalcRequest.builder()
                    .startDateIncl(periodStartDateIncl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(periodStartValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(periodFlows)
                    .assetValues(assetValues)
                    .flowTiming(flowTiming)
                    .calcScale(calcScale)
                    .resultScale(calcScale)
                    .roundingMode(roundingMode)
                    .build();
            BigDecimal periodTwr = twrCalculator.calculateReturn(periodTwrPerfCalcReq);
            cumulTwrFactor = cumulTwrFactor.multiply(periodTwr.add(ONE)).setScale(calcScale, roundingMode);
            BigDecimal annTwrFactor = annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, cumulTwrFactor, startDateIncl, periodEndDateIncl);

            PerfCalcRequest cumulMwrPerfCalcReq = PerfCalcRequest.builder()
                    .startDateIncl(startDateIncl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(startValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(periodFlows)
                    .flowTiming(flowTiming)
                    .calcScale(calcScale)
                    .resultScale(calcScale)
                    .roundingMode(roundingMode)
                    .build();
            BigDecimal periodMwr = periodTwr;
            BigDecimal cumulMwr = mwrCalculator.calculateReturn(cumulMwrPerfCalcReq);
            BigDecimal annMwr = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, cumulMwr, startDateIncl, periodEndDateIncl);

            BigDecimal periodFlowSum = periodTwrPerfCalcReq.flows().values().stream().reduce(ZERO, BigDecimal::add);

            results.add(PerfAnalysis.builder()
                    .periodStartDateIncl(periodStartDateIncl)
                    .periodEndDateIncl(periodEndDateIncl)
                    .periodStartAssetValueExcl(periodStartValueExcl)
                    .periodEndAssetValueIncl(periodEndValueIncl)
                    .flowSum(periodFlowSum)
                    .periodTwr(periodTwr.setScale(resultScale, roundingMode))
                    .cumulativeTwr(cumulTwrFactor.subtract(ONE).setScale(resultScale, roundingMode))
                    .annualizedTwr(annTwrFactor.subtract(ONE).setScale(resultScale, roundingMode))
                    .periodMwr(periodMwr.setScale(resultScale, roundingMode))
                    .cumulativeMwr(cumulMwr.setScale(resultScale, roundingMode))
                    .annualizedMwr(annMwr.setScale(resultScale, roundingMode))
                    .build());

            periodStartDateIncl = periodEndDateIncl.plusDays(1);
        }
        return results;
    }
}
