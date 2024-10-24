package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.LinkedModifiedDietzTwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.ModifiedDietzMwrCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.TreeMap;

import static com.brinvex.fintracker.core.api.model.general.PeriodUnit.MONTH;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.test.support.AssertionUtil.assertEqualsWithMultilineMsg;
import static com.brinvex.fintracker.test.support.CollectionStringUtil.collectionToGridString;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;

public class PerformanceAnalyzerTest {

    private static final FinTracker finTracker = FinTracker.newInstance();

    private static final PerformanceModule perfModule = finTracker.get(PerformanceModule.class);

    private static final PerformanceAnalyzer perfAnalyzer = perfModule.performanceAnalyzer();

    private static String perfAnalysesToGridString(SequencedCollection<PerfAnalysis> perfAnalyses) {
        return collectionToGridString(perfAnalyses,
                List.of(
                        "period",
                        "startVal",
                        "endVal",
                        "prdFlow",
                        "prdTwr",
                        "cumTwr",
                        "annTwr",
                        "cumMwr",
                        "annMwr",
                        "totContrib",
                        "prdProf",
                        "totProf",
                        "trlAvgProf1Y",
                        "trlAvgFlow1Y",
                        "prdIncm",
                        "trlAvgIncm1Y",
                        "trlTwr1Y",
                        "trlTwr2Y",
                        "trlTwr3Y",
                        "trlTwr5Y",
                        "trlTwr10Y"
                ),
                List.of(
                        PerfAnalysis::periodCaption,
                        PerfAnalysis::periodStartAssetValueExcl,
                        PerfAnalysis::periodEndAssetValueIncl,
                        PerfAnalysis::periodFlow,
                        PerfAnalysis::periodTwr,
                        PerfAnalysis::cumulativeTwr,
                        PerfAnalysis::annualizedTwr,
                        PerfAnalysis::cumulativeMwr,
                        PerfAnalysis::annualizedMwr,
                        PerfAnalysis::totalContribution,
                        PerfAnalysis::periodProfit,
                        PerfAnalysis::totalProfit,
                        PerfAnalysis::trailingAvgProfit1Y,
                        PerfAnalysis::trailingAvgFlow1Y,
                        PerfAnalysis::periodIncome,
                        PerfAnalysis::trailingAvgIncome1Y,
                        PerfAnalysis::trailingTwr1Y,
                        PerfAnalysis::trailingTwr2Y,
                        PerfAnalysis::trailingTwr3Y,
                        PerfAnalysis::trailingTwr5Y,
                        PerfAnalysis::trailingTwr10Y
                )
        );
    }

    @Test
    void readmeExample() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "100000"),
                        new DateAmount("2023-01-31", "98000"),
                        new DateAmount("2023-02-28", "117000"),
                        new DateAmount("2023-03-31", "120000")
                ))
                .flows(List.of(
                        new DateAmount("2023-01-20", "2000"),
                        new DateAmount("2023-02-15", "1000"),
                        new DateAmount("2023-02-07", "-1500")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .mwrCalculatorType(ModifiedDietzMwrCalculator.class)
                .resultPeriodUnit(MONTH)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .calculateTrailingAvgProfit1Y(true)
                .calculateTrailingAvgFlow1Y(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;   100000;  98000;    2000;  -3.97;  -3.97;  -3.97;  -3.97;  -3.97;     102000;   -4000;   -4000;        -4000;         2000;    null;         null;     null;     null;     null;     null;      null
                2023-02;    98000; 117000;    -500;  20.04;  15.27;  15.27;  15.34;  15.34;     101500;   19500;   15500;         7750;          750;    null;         null;     null;     null;     null;     null;      null
                2023-03;   117000; 120000;       0;   2.56;  18.23;  18.23;  18.28;  18.28;     101500;    3000;   18500;         6167;          500;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance1() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-01-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "10000"),
                        new DateAmount("2023-01-31", "10500")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;    10000;  10500;       0;   5.00;   5.00;   5.00;   5.00;   5.00;      10000;     500;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance2() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "10000"),
                        new DateAmount("2023-01-31", "10500"),
                        new DateAmount("2023-02-28", "10500")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;    10000;  10500;       0;   5.00;   5.00;   5.00;   5.00;   5.00;      10000;     500;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;    10500;  10500;       0;   0.00;   5.00;   5.00;   5.00;   5.00;      10000;       0;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance3() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "0")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance4() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "1000"),
                        new DateAmount("2023-02-28", "0")
                ))
                .flows(List.of(
                        new DateAmount("2023-01-01", "500")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow;  prdTwr;  cumTwr;  annTwr;  cumMwr;  annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;   1000;     500;  100.00;  100.00;  100.00;  100.00;  100.00;        500;     500;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1000;      0;       0; -100.00; -100.00; -100.00; -100.00; -100.00;        500;   -1000;    -500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance5() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "1000"),
                        new DateAmount("2023-02-28", "1000")
                ))
                .flows(List.of(
                        new DateAmount("2023-01-01", "500")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;   1000;     500; 100.00; 100.00; 100.00; 100.00; 100.00;        500;     500;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1000;   1000;       0;   0.00; 100.00; 100.00; 100.00; 100.00;        500;       0;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance6() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "500"),
                        new DateAmount("2023-01-31", "1000"),
                        new DateAmount("2023-02-28", "1000")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .resultRatesInPercent(true)
                .calculateMwr(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;      500;   1000;       0; 100.00; 100.00; 100.00; 100.00; 100.00;        500;     500;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1000;   1000;       0;   0.00; 100.00; 100.00; 100.00; 100.00;        500;       0;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }


    @Test
    void analyzePerformance7() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "1000")
                ))
                .flows(List.of(
                        new DateAmount("2023-02-01", "500")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;        0;   1000;     500; 100.00; 100.00; 100.00; 100.00; 100.00;        500;     500;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance8() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "1000")
                ))
                .flows(List.of(
                        new DateAmount("2023-02-15", "500")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;        0;   1000;     500; 100.00; 100.00; 100.00; 100.00; 100.00;        500;     500;     500;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance9() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "1"),
                        new DateAmount("2023-01-31", "1"),
                        new DateAmount("2023-02-14", "2"),
                        new DateAmount("2023-02-28", "1000")
                ))
                .flows(List.of(
                        new DateAmount("2023-02-15", "998")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        1;      1;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          1;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;        1;   1000;     998; 100.00; 100.00; 100.00;   0.42;   0.42;        999;       1;       1;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance10() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "1"),
                        new DateAmount("2023-01-31", "1"),
                        new DateAmount("2023-02-14", "2"),
                        new DateAmount("2023-02-28", "1000"),
                        new DateAmount("2023-03-31", "1000")
                ))
                .flows(new TreeMap<>(Map.of(
                        parse("2023-02-15"), new BigDecimal("998"),
                        parse("2024-02-15"), new BigDecimal("6000000")
                )))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(2)
                .calculateMwr(true)
                .calculateTrailingAvgProfit1Y(true)
                .calculateTrailingAvgFlow1Y(true)
                .build());
        String expected = """
                 period; startVal;  endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     1.00;    1.00;    0.00;   0.00;   0.00;   0.00;   0.00;   0.00;       1.00;    0.00;    0.00;         0.00;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1.00; 1000.00;  998.00; 100.00; 100.00; 100.00;   0.42;   0.42;     999.00;    1.00;    1.00;         0.50;       499.00;    null;         null;     null;     null;     null;     null;      null
                2023-03;  1000.00; 1000.00;    0.00;   0.00; 100.00; 100.00;   0.20;   0.20;     999.00;    0.00;    1.00;         0.33;       332.67;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }


    @Test
    void analyzePerformance11() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2024-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "1"),
                        new DateAmount("2023-01-31", "1"),
                        new DateAmount("2023-02-28", "2"),
                        new DateAmount("2023-03-31", "2"),
                        new DateAmount("2023-04-30", "2"),
                        new DateAmount("2023-05-31", "2"),
                        new DateAmount("2023-06-30", "2"),
                        new DateAmount("2023-07-31", "2"),
                        new DateAmount("2023-08-31", "2"),
                        new DateAmount("2023-09-30", "2"),
                        new DateAmount("2023-10-31", "2"),
                        new DateAmount("2023-11-30", "2"),
                        new DateAmount("2023-12-31", "2"),
                        new DateAmount("2024-01-31", "2"),
                        new DateAmount("2024-02-29", "2"),
                        new DateAmount("2024-03-31", "2")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(2)
                .calculateMwr(true)
                .calculateTrailingAvgProfit1Y(true)
                .calculateTrailingAvgFlow1Y(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     1.00;   1.00;    0.00;   0.00;   0.00;   0.00;   0.00;   0.00;       1.00;    0.00;    0.00;         0.00;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1.00;   2.00;    0.00; 100.00; 100.00; 100.00; 100.00; 100.00;       1.00;    1.00;    1.00;         0.50;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-03;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.33;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-04;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.25;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-05;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.20;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-06;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.17;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-07;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.14;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-08;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.13;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-09;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.11;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-10;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.10;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-11;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.09;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-12;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.08;         0.00;    null;         null;     null;     null;     null;     null;      null
                2024-01;     2.00;   2.00;    0.00;   0.00; 100.00;  89.44; 100.00;  89.44;       1.00;    0.00;    1.00;         0.08;         0.00;    null;         null;     null;     null;     null;     null;      null
                2024-02;     2.00;   2.00;    0.00;   0.00; 100.00;  81.36; 100.00;  81.36;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     null;     null;     null;     null;      null
                2024-03;     2.00;   2.00;    0.00;   0.00; 100.00;  74.16; 100.00;  74.16;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance12() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2024-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "1"),
                        new DateAmount("2023-01-31", "1"),
                        new DateAmount("2023-02-28", "2"),
                        new DateAmount("2023-03-31", "2"),
                        new DateAmount("2023-04-30", "2"),
                        new DateAmount("2023-05-31", "2"),
                        new DateAmount("2023-06-30", "2"),
                        new DateAmount("2023-07-31", "2"),
                        new DateAmount("2023-08-31", "2"),
                        new DateAmount("2023-09-30", "2"),
                        new DateAmount("2023-10-31", "2"),
                        new DateAmount("2023-11-30", "2"),
                        new DateAmount("2023-12-31", "2"),
                        new DateAmount("2024-01-31", "2"),
                        new DateAmount("2024-02-29", "2"),
                        new DateAmount("2024-03-31", "2")
                ))
                .incomes(List.of(
                        new DateAmount("2023-12-31", "48"),
                        new DateAmount("2024-01-13", "12")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(2)
                .calculateMwr(true)
                .calculateTrailingAvgProfit1Y(true)
                .calculateTrailingAvgFlow1Y(true)
                .calculatePeriodIncome(true)
                .calculateTrailingAvgIncome1Y(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     1.00;   1.00;    0.00;   0.00;   0.00;   0.00;   0.00;   0.00;       1.00;    0.00;    0.00;         0.00;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-02;     1.00;   2.00;    0.00; 100.00; 100.00; 100.00; 100.00; 100.00;       1.00;    1.00;    1.00;         0.50;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-03;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.33;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-04;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.25;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-05;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.20;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-06;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.17;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-07;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.14;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-08;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.13;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-09;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.11;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-10;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.10;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-11;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.09;         0.00;    0.00;         0.00;     null;     null;     null;     null;      null
                2023-12;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.08;         0.00;   48.00;         4.00;     null;     null;     null;     null;      null
                2024-01;     2.00;   2.00;    0.00;   0.00; 100.00;  89.44; 100.00;  89.44;       1.00;    0.00;    1.00;         0.08;         0.00;   12.00;         5.00;     null;     null;     null;     null;      null
                2024-02;     2.00;   2.00;    0.00;   0.00; 100.00;  81.36; 100.00;  81.36;       1.00;    0.00;    1.00;         0.00;         0.00;    0.00;         5.00;     null;     null;     null;     null;      null
                2024-03;     2.00;   2.00;    0.00;   0.00; 100.00;  74.16; 100.00;  74.16;       1.00;    0.00;    1.00;         0.00;         0.00;    0.00;         5.00;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance13() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2024-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "1"),
                        new DateAmount("2023-01-31", "1"),
                        new DateAmount("2023-02-28", "1.5"),
                        new DateAmount("2023-03-31", "2.25"),
                        new DateAmount("2023-04-30", "2.025"),
                        new DateAmount("2023-05-31", "2"),
                        new DateAmount("2023-06-30", "2"),
                        new DateAmount("2023-07-31", "2"),
                        new DateAmount("2023-08-31", "2"),
                        new DateAmount("2023-09-30", "2"),
                        new DateAmount("2023-10-31", "2"),
                        new DateAmount("2023-11-30", "2"),
                        new DateAmount("2023-12-31", "2"),
                        new DateAmount("2024-01-31", "2"),
                        new DateAmount("2024-02-29", "2"),
                        new DateAmount("2024-03-31", "2")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(2)
                .calculateMwr(true)
                .calculateTrailingAvgProfit1Y(true)
                .calculateTrailingAvgFlow1Y(true)
                .calculateTrailingTwr1Y(true)
                .calculateTrailingTwr2Y(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     1.00;   1.00;    0.00;   0.00;   0.00;   0.00;   0.00;   0.00;       1.00;    0.00;    0.00;         0.00;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1.00;   1.50;    0.00;  50.00;  50.00;  50.00;  50.00;  50.00;       1.00;    0.50;    0.50;         0.25;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-03;     1.50;   2.25;    0.00;  50.00; 125.00; 125.00; 125.00; 125.00;       1.00;    0.75;    1.25;         0.42;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-04;     2.25;   2.03;    0.00; -10.00; 102.50; 102.50; 102.50; 102.50;       1.00;   -0.23;    1.03;         0.26;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-05;     2.03;   2.00;    0.00;  -1.23; 100.00; 100.00; 100.00; 100.00;       1.00;   -0.03;    1.00;         0.20;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-06;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.17;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-07;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.14;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-08;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.13;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-09;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.11;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-10;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.10;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-11;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.09;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-12;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.08;         0.00;    null;         null;   100.00;     null;     null;     null;      null
                2024-01;     2.00;   2.00;    0.00;   0.00; 100.00;  89.44; 100.00;  89.44;       1.00;    0.00;    1.00;         0.08;         0.00;    null;         null;   100.00;     null;     null;     null;      null
                2024-02;     2.00;   2.00;    0.00;   0.00; 100.00;  81.36; 100.00;  81.36;       1.00;    0.00;    1.00;         0.04;         0.00;    null;         null;    33.33;     null;     null;     null;      null
                2024-03;     2.00;   2.00;    0.00;   0.00; 100.00;  74.16; 100.00;  74.16;       1.00;    0.00;    1.00;        -0.02;         0.00;    null;         null;   -11.11;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance14() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2025-01-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "1"),
                        new DateAmount("2023-01-31", "1"),
                        new DateAmount("2023-02-28", "1.5"),
                        new DateAmount("2023-03-31", "2.25"),
                        new DateAmount("2023-04-30", "2.025"),
                        new DateAmount("2023-05-31", "2"),
                        new DateAmount("2023-06-30", "2"),
                        new DateAmount("2023-07-31", "2"),
                        new DateAmount("2023-08-31", "2"),
                        new DateAmount("2023-09-30", "2"),
                        new DateAmount("2023-10-31", "2"),
                        new DateAmount("2023-11-30", "2"),
                        new DateAmount("2023-12-31", "2"),
                        new DateAmount("2024-01-31", "2"),
                        new DateAmount("2024-02-29", "2"),
                        new DateAmount("2024-03-31", "2"),
                        new DateAmount("2024-04-30", "2"),
                        new DateAmount("2024-05-31", "2"),
                        new DateAmount("2024-06-30", "2"),
                        new DateAmount("2024-07-31", "2"),
                        new DateAmount("2024-08-31", "2"),
                        new DateAmount("2024-09-30", "2"),
                        new DateAmount("2024-10-31", "2"),
                        new DateAmount("2024-11-30", "2"),
                        new DateAmount("2024-12-31", "2"),
                        new DateAmount("2025-01-31", "2.2")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(2)
                .calculateMwr(true)
                .calculateTrailingAvgProfit1Y(true)
                .calculateTrailingAvgFlow1Y(true)
                .calculateTrailingTwr1Y(true)
                .calculateTrailingTwr2Y(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     1.00;   1.00;    0.00;   0.00;   0.00;   0.00;   0.00;   0.00;       1.00;    0.00;    0.00;         0.00;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1.00;   1.50;    0.00;  50.00;  50.00;  50.00;  50.00;  50.00;       1.00;    0.50;    0.50;         0.25;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-03;     1.50;   2.25;    0.00;  50.00; 125.00; 125.00; 125.00; 125.00;       1.00;    0.75;    1.25;         0.42;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-04;     2.25;   2.03;    0.00; -10.00; 102.50; 102.50; 102.50; 102.50;       1.00;   -0.23;    1.03;         0.26;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-05;     2.03;   2.00;    0.00;  -1.23; 100.00; 100.00; 100.00; 100.00;       1.00;   -0.03;    1.00;         0.20;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-06;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.17;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-07;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.14;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-08;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.13;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-09;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.11;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-10;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.10;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-11;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.09;         0.00;    null;         null;     null;     null;     null;     null;      null
                2023-12;     2.00;   2.00;    0.00;   0.00; 100.00; 100.00; 100.00; 100.00;       1.00;    0.00;    1.00;         0.08;         0.00;    null;         null;   100.00;     null;     null;     null;      null
                2024-01;     2.00;   2.00;    0.00;   0.00; 100.00;  89.44; 100.00;  89.44;       1.00;    0.00;    1.00;         0.08;         0.00;    null;         null;   100.00;     null;     null;     null;      null
                2024-02;     2.00;   2.00;    0.00;   0.00; 100.00;  81.36; 100.00;  81.36;       1.00;    0.00;    1.00;         0.04;         0.00;    null;         null;    33.33;     null;     null;     null;      null
                2024-03;     2.00;   2.00;    0.00;   0.00; 100.00;  74.16; 100.00;  74.16;       1.00;    0.00;    1.00;        -0.02;         0.00;    null;         null;   -11.11;     null;     null;     null;      null
                2024-04;     2.00;   2.00;    0.00;   0.00; 100.00;  68.30; 100.00;  68.30;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;    -1.23;     null;     null;     null;      null
                2024-05;     2.00;   2.00;    0.00;   0.00; 100.00;  63.13; 100.00;  63.13;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;     null;     null;     null;      null
                2024-06;     2.00;   2.00;    0.00;   0.00; 100.00;  58.81; 100.00;  58.81;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;     null;     null;     null;      null
                2024-07;     2.00;   2.00;    0.00;   0.00; 100.00;  54.92; 100.00;  54.92;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;     null;     null;     null;      null
                2024-08;     2.00;   2.00;    0.00;   0.00; 100.00;  51.50; 100.00;  51.50;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;     null;     null;     null;      null
                2024-09;     2.00;   2.00;    0.00;   0.00; 100.00;  48.58; 100.00;  48.58;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;     null;     null;     null;      null
                2024-10;     2.00;   2.00;    0.00;   0.00; 100.00;  45.88; 100.00;  45.88;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;     null;     null;     null;      null
                2024-11;     2.00;   2.00;    0.00;   0.00; 100.00;  43.54; 100.00;  43.54;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;     null;     null;     null;      null
                2024-12;     2.00;   2.00;    0.00;   0.00; 100.00;  41.42; 100.00;  41.42;       1.00;    0.00;    1.00;         0.00;         0.00;    null;         null;     0.00;    41.42;     null;     null;      null
                2025-01;     2.00;   2.20;    0.00;  10.00; 120.00;  45.96; 120.00;  45.96;       1.00;    0.20;    1.20;         0.02;         0.00;    null;         null;    10.00;    48.32;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance15() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "0"),
                        new DateAmount("2023-03-31", "0")
                ))
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-03;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance16() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2023-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "1000"),
                        new DateAmount("2023-02-28", "0"),
                        new DateAmount("2023-03-31", "0")
                ))
                .flows(List.of(
                        new DateAmount("2023-01-01", "1000"),
                        new DateAmount("2023-02-01", "-1000")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;   1000;    1000;   0.00;   0.00;   0.00;   0.00;   0.00;       1000;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;     1000;      0;   -1000;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-03;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance17() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2024-03-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "0"),
                        new DateAmount("2023-03-31", "0"),
                        new DateAmount("2023-04-30", "0"),
                        new DateAmount("2023-05-31", "0"),
                        new DateAmount("2023-06-30", "0"),
                        new DateAmount("2023-07-31", "0"),
                        new DateAmount("2023-08-31", "0"),
                        new DateAmount("2023-09-30", "0"),
                        new DateAmount("2023-10-31", "0"),
                        new DateAmount("2023-11-30", "0"),
                        new DateAmount("2023-12-31", "0"),
                        new DateAmount("2024-01-31", "1100"),
                        new DateAmount("2024-02-29", "1100"),
                        new DateAmount("2024-03-31", "1100")
                ))
                .flows(List.of(
                        new DateAmount("2024-01-01", "1000")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-03;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-04;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-05;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-06;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-07;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-08;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-09;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-10;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-11;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-12;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-01;        0;   1100;    1000;  10.00;  10.00;   9.18;  10.00;   9.18;       1000;     100;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-02;     1100;   1100;       0;   0.00;  10.00;   8.53;  10.00;   8.53;       1000;       0;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-03;     1100;   1100;       0;   0.00;  10.00;   7.93;  10.00;   7.93;       1000;       0;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance18() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2024-03-31"))
                .investmentStartDateIncl(parse("2023-08-01"))
                .investmentEndDateIncl(now())
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "0"),
                        new DateAmount("2023-03-31", "0"),
                        new DateAmount("2023-04-30", "0"),
                        new DateAmount("2023-05-31", "0"),
                        new DateAmount("2023-06-30", "0"),
                        new DateAmount("2023-07-31", "0"),
                        new DateAmount("2023-08-31", "0"),
                        new DateAmount("2023-09-30", "0"),
                        new DateAmount("2023-10-31", "0"),
                        new DateAmount("2023-11-30", "0"),
                        new DateAmount("2023-12-31", "0"),
                        new DateAmount("2024-01-31", "1100"),
                        new DateAmount("2024-02-29", "1100"),
                        new DateAmount("2024-03-31", "1100")
                ))
                .flows(List.of(
                        new DateAmount("2024-01-01", "1000")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-03;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-04;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-05;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-06;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-07;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-08;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-09;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-10;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-11;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-12;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-01;        0;   1100;    1000;  10.00;  10.00;  10.00;  10.00;  10.00;       1000;     100;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-02;     1100;   1100;       0;   0.00;  10.00;  10.00;  10.00;  10.00;       1000;       0;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-03;     1100;   1100;       0;   0.00;  10.00;  10.00;  10.00;  10.00;       1000;       0;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance19() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2024-03-31"))
                .investmentStartDateIncl(parse("2024-08-01"))
                .investmentEndDateIncl(now())
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "0"),
                        new DateAmount("2023-03-31", "0"),
                        new DateAmount("2023-04-30", "0"),
                        new DateAmount("2023-05-31", "0"),
                        new DateAmount("2023-06-30", "0"),
                        new DateAmount("2023-07-31", "0"),
                        new DateAmount("2023-08-31", "0"),
                        new DateAmount("2023-09-30", "0"),
                        new DateAmount("2023-10-31", "0"),
                        new DateAmount("2023-11-30", "0"),
                        new DateAmount("2023-12-31", "0"),
                        new DateAmount("2024-01-31", "1100"),
                        new DateAmount("2024-02-29", "1100"),
                        new DateAmount("2024-03-31", "1100")
                ))
                .flows(List.of(
                        new DateAmount("2024-01-01", "1000")
                ))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-03;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-04;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-05;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-06;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-07;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-08;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-09;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-10;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-11;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-12;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-01;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-02;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-03;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-04;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-05;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-06;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-07;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance20() {
        SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .analysisStartDateIncl(parse("2023-01-01"))
                .analysisEndDateIncl(parse("2024-03-31"))
                .investmentStartDateIncl(parse("2023-08-01"))
                .investmentEndDateIncl(now())
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "0"),
                        new DateAmount("2023-03-31", "0"),
                        new DateAmount("2023-04-30", "0"),
                        new DateAmount("2023-05-31", "0"),
                        new DateAmount("2023-06-30", "0"),
                        new DateAmount("2023-07-31", "0"),
                        new DateAmount("2023-08-31", "0"),
                        new DateAmount("2023-09-30", "0"),
                        new DateAmount("2023-10-31", "0"),
                        new DateAmount("2023-11-30", "0"),
                        new DateAmount("2023-12-31", "0"),
                        new DateAmount("2024-01-31", "1100"),
                        new DateAmount("2024-02-29", "1100"),
                        new DateAmount("2024-03-31", "1100")
                ))
                .flows((_, _) -> new TreeMap<>(Map.of(parse("2024-01-01"), new BigDecimal("1000"))))
                .twrFlowTiming(BEGINNING_OF_DAY)
                .twrCalculatorType(LinkedModifiedDietzTwrCalculator.class)
                .resultRatesInPercent(true)
                .resultRateScale(2)
                .resultAmountScale(0)
                .calculateMwr(true)
                .build());
        String expected = """
                 period; startVal; endVal; prdFlow; prdTwr; cumTwr; annTwr; cumMwr; annMwr; totContrib; prdProf; totProf; trlAvgProf1Y; trlAvgFlow1Y; prdIncm; trlAvgIncm1Y; trlTwr1Y; trlTwr2Y; trlTwr3Y; trlTwr5Y; trlTwr10Y
                2023-01;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-02;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-03;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-04;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-05;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-06;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-07;     null;   null;    null;   null;   null;   null;   null;   null;       null;    null;    null;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-08;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-09;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-10;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-11;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2023-12;        0;      0;       0;   0.00;   0.00;   0.00;   0.00;   0.00;          0;       0;       0;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-01;        0;   1100;    1000;  10.00;  10.00;  10.00;  10.00;  10.00;       1000;     100;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-02;     1100;   1100;       0;   0.00;  10.00;  10.00;  10.00;  10.00;       1000;       0;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                2024-03;     1100;   1100;       0;   0.00;  10.00;  10.00;  10.00;  10.00;       1000;       0;     100;         null;         null;    null;         null;     null;     null;     null;     null;      null
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

}
