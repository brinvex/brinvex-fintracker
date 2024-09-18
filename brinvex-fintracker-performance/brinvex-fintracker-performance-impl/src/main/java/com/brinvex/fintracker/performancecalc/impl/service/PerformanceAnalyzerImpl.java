package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.MwrCalcMethod;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.TwrCalcMethod;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.validation.Validate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeGrowthFactor;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeReturn;
import static com.brinvex.util.java.DateUtil.minDate;
import static com.brinvex.util.java.NullUtil.nullSafe;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("DuplicatedCode")
public class PerformanceAnalyzerImpl implements PerformanceAnalyzer {

    private final PerformanceCalculator perfCalculator;

    public PerformanceAnalyzerImpl(PerformanceCalculator perfCalculator) {
        this.perfCalculator = perfCalculator;
    }

    @Override
    public List<PerfAnalysis> analyzePerformance(PerfAnalysisRequest perfAnalysisReq) {
        LocalDate startDateIncl = perfAnalysisReq.startDateIncl();
        LocalDate endDateIncl = perfAnalysisReq.endDateIncl();
        TwrCalcMethod twrCalcMethod = perfAnalysisReq.twrCalcMethod();
        MwrCalcMethod mwrCalcMethod = perfAnalysisReq.mwrCalcMethod();
        FlowTiming flowTiming = perfAnalysisReq.flowTiming();
        PeriodUnit periodUnit = perfAnalysisReq.resultPeriodUnit();

        SortedMap<LocalDate, DateAmount> flows = perfAnalysisReq.cashFlows()
                .stream()
                .collect(toMap(DateAmount::date, identity(), DateAmount::add, TreeMap::new));

        Map<LocalDate, DateAmount> assetValues = perfAnalysisReq.assetValues()
                .stream()
                .collect(toMap(DateAmount::date, identity()));

        if (periodUnit == PeriodUnit.DAY) {
            return analyzeDailyPerformance(startDateIncl, endDateIncl, flows, assetValues, twrCalcMethod, mwrCalcMethod, flowTiming);
        }

        LocalDate startDateExcl = startDateIncl.minusDays(1);
        BigDecimal startValueExcl = nullSafe(assetValues.get(startDateExcl), DateAmount::amount);
        Validate.notNull(startValueExcl, () -> "startAssetValueExcl must not be null, missing asset value for startDateExcl=%s"
                .formatted(startDateExcl));

        LocalDate periodStartDateIncl = startDateIncl;
        List<PerfAnalysis> results = new ArrayList<>();
        BigDecimal cumulTwrFactor = ONE;
        while (!periodStartDateIncl.isAfter(endDateIncl)) {
            LocalDate periodStartDateExcl = periodStartDateIncl.minusDays(1);
            LocalDate periodEndDateIncl = minDate(periodUnit.adjEndDateIncl(periodStartDateIncl), endDateIncl);
            LocalDate periodEndDateExcl = periodEndDateIncl.plusDays(1);
            BigDecimal periodStartValueExcl = nullSafe(assetValues.get(periodStartDateExcl), DateAmount::amount);
            Validate.notNull(periodStartValueExcl, () -> "periodStartValueExcl must not be null, missing asset value for periodStartDateExcl=%s"
                    .formatted(periodStartDateExcl));
            BigDecimal periodEndValueIncl = nullSafe(assetValues.get(endDateIncl), DateAmount::amount);
            Validate.notNull(periodEndValueIncl, () -> "periodEndValueIncl must not be null, missing asset value for endDateIncl=%s"
                    .formatted(periodEndDateIncl));

            SortedMap<LocalDate, DateAmount> periodFlows = flows.subMap(periodStartDateIncl, periodEndDateExcl);
            BigDecimal periodFlowSum = periodFlows.values().stream().map(DateAmount::amount).reduce(ZERO, BigDecimal::add);

            BigDecimal periodMwr = perfCalculator.calculateReturn(PerfCalcRequest.builder()
                    .calcMethod(mwrCalcMethod)
                    .startDateIncl(periodStartDateExcl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(periodStartValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(periodFlows.values())
                    .assetValues(assetValues.values())
                    .flowTiming(flowTiming)
                    .annualization(DO_NOT_ANNUALIZE)
                    .build());
            BigDecimal cumulMwr = perfCalculator.calculateReturn(PerfCalcRequest.builder()
                    .calcMethod(mwrCalcMethod)
                    .startDateIncl(startDateIncl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(startValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(flows.values())
                    .assetValues(assetValues.values())
                    .flowTiming(flowTiming)
                    .annualization(DO_NOT_ANNUALIZE)
                    .build());
            BigDecimal annMwr = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, cumulMwr, startDateIncl, periodEndDateIncl);

            BigDecimal periodTwr;
            if (twrCalcMethod == TwrCalcMethod.LINKED_MODIFIED_DIETZ && periodUnit == PeriodUnit.MONTH) {
                periodTwr = periodMwr;
            } else {
                periodTwr = perfCalculator.calculateReturn(PerfCalcRequest.builder()
                        .calcMethod(twrCalcMethod)
                        .startDateIncl(periodStartDateExcl)
                        .endDateIncl(periodEndDateIncl)
                        .startAssetValueExcl(periodStartValueExcl)
                        .endAssetValueIncl(periodEndValueIncl)
                        .flows(periodFlows.values())
                        .assetValues(assetValues.values())
                        .flowTiming(flowTiming)
                        .annualization(DO_NOT_ANNUALIZE)
                        .build());
            }
            cumulTwrFactor = cumulTwrFactor.multiply(periodTwr.add(ONE));
            BigDecimal annTwrFactor = annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, cumulTwrFactor, startDateIncl, periodEndDateIncl);

            results.add(PerfAnalysis.builder()
                    .periodStartDateIncl(periodStartDateIncl)
                    .periodEndDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(periodStartValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flow(periodFlowSum)
                    .periodTwr(periodTwr)
                    .cumulativeTwr(cumulTwrFactor.subtract(ONE))
                    .annualizedTwr(annTwrFactor.subtract(ONE))
                    .periodMwr(periodMwr)
                    .cumulativeMwr(cumulMwr)
                    .annualizedMwr(annMwr)
                    .build());

            periodStartDateIncl = periodEndDateIncl.plusDays(1);
        }
        return results;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private List<PerfAnalysis> analyzeDailyPerformance(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            SortedMap<LocalDate, DateAmount> flows,
            Map<LocalDate, DateAmount> assetValues,
            RateOfReturnCalcMethod twrCalcMethod,
            RateOfReturnCalcMethod mwrCalcMethod,
            FlowTiming flowTiming
    ) {

        List<PerfAnalysis> results = new ArrayList<>();
        BigDecimal cumulTwrFactor = ONE;

        LocalDate startDateExcl = startDateIncl.minusDays(1);
        BigDecimal startValueExcl = nullSafe(assetValues.get(startDateExcl), DateAmount::amount);
        Validate.notNull(startValueExcl, () -> "startAssetValueExcl must not be null, missing asset value for startDateExcl=%s"
                .formatted(startDateExcl));

        LocalDate periodStartDateIncl = startDateIncl;
        while (!periodStartDateIncl.isAfter(endDateIncl)) {
            LocalDate periodStartDateExcl = periodStartDateIncl.minusDays(1);
            LocalDate periodEndDateIncl = periodStartDateIncl;
            LocalDate periodEndDateExcl = periodStartDateIncl.plusDays(1);
            BigDecimal periodStartValueExcl = nullSafe(assetValues.get(periodStartDateExcl), DateAmount::amount);
            Validate.notNull(periodStartValueExcl, () -> "periodStartValueExcl must not be null, missing asset value for periodStartDateExcl=%s"
                    .formatted(periodStartDateExcl));
            BigDecimal periodEndValueIncl = nullSafe(assetValues.get(endDateIncl), DateAmount::amount);
            Validate.notNull(periodEndValueIncl, () -> "periodEndValueIncl must not be null, missing asset value for endDateIncl=%s"
                    .formatted(periodEndDateIncl));

            SortedMap<LocalDate, DateAmount> periodFlows = flows.subMap(periodStartDateIncl, periodEndDateExcl);
            BigDecimal periodFlowSum = periodFlows.values().stream().map(DateAmount::amount).reduce(ZERO, BigDecimal::add);

            BigDecimal periodTwr = perfCalculator.calculateReturn(PerfCalcRequest.builder()
                    .calcMethod(twrCalcMethod)
                    .startDateIncl(periodStartDateIncl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(periodStartValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(periodFlows.values())
                    .assetValues(assetValues.values())
                    .flowTiming(flowTiming)
                    .build());
            cumulTwrFactor = cumulTwrFactor.multiply(periodTwr.add(ONE));
            BigDecimal annTwrFactor = annualizeGrowthFactor(ANNUALIZE_IF_OVER_ONE_YEAR, cumulTwrFactor, startDateIncl, periodEndDateIncl);

            BigDecimal periodMwr = periodTwr;
            BigDecimal cumulMwr = perfCalculator.calculateReturn(PerfCalcRequest.builder()
                    .calcMethod(mwrCalcMethod)
                    .startDateIncl(startDateIncl)
                    .endDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(startValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flows(flows.values())
                    .flowTiming(flowTiming)
                    .build());
            BigDecimal annMwr = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, cumulMwr, startDateIncl, periodEndDateIncl);

            results.add(PerfAnalysis.builder()
                    .periodStartDateIncl(periodStartDateIncl)
                    .periodEndDateIncl(periodEndDateIncl)
                    .startAssetValueExcl(periodStartValueExcl)
                    .endAssetValueIncl(periodEndValueIncl)
                    .flow(periodFlowSum)
                    .periodTwr(periodTwr)
                    .cumulativeTwr(cumulTwrFactor.subtract(ONE))
                    .annualizedTwr(annTwrFactor.subtract(ONE))
                    .periodMwr(periodMwr)
                    .cumulativeMwr(cumulMwr)
                    .annualizedMwr(annMwr)
                    .build());

            periodStartDateIncl = periodEndDateIncl.plusDays(1);
        }
        return results;
    }
}
