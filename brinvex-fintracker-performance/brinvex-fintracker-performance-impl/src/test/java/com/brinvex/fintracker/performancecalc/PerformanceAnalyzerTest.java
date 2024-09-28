package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.test.support.CollectionStringUtil.collectionToGridString;
import static com.brinvex.fintracker.test.support.AssertionUtil.assertEqualsWithMultilineMsg;
import static java.time.LocalDate.parse;

public class PerformanceAnalyzerTest {

    private static final FinTracker finTracker = FinTracker.newInstance();

    private static final PerformanceModule perfModule = finTracker.get(PerformanceModule.class);

    private static final PerformanceAnalyzer perfAnalyzer = perfModule.performanceAnalyzer();

    @Test
    void analyzePerformance1() {
        List<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2023-01-31"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "10000"),
                        new DateAmount("2023-01-31", "10500")
                ))
                .resultRatesInPercent(true)
                .resultScale(2)
                .build());
        String expected = """
                 period; startVal; endVal; periodFlow; periodTwr; cumulTwr; annTwr; periodMwr; cumulMwr; annMwr
                2023-01;    10000;  10500;          0;      5.00;     5.00;   5.00;      5.00;     5.00;   5.00
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance2() {
        List<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "10000"),
                        new DateAmount("2023-01-31", "10500"),
                        new DateAmount("2023-02-28", "10500")
                ))
                .resultRatesInPercent(true)
                .resultScale(2)
                .build());
        String expected = """
                 period; startVal; endVal; periodFlow; periodTwr; cumulTwr; annTwr; periodMwr; cumulMwr; annMwr
                2023-01;    10000;  10500;          0;      5.00;     5.00;   5.00;      5.00;     5.00;   5.00
                2023-02;    10500;  10500;          0;      0.00;     5.00;   5.00;      0.00;     5.00;   5.00
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance3() {
        List<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "0"),
                        new DateAmount("2023-02-28", "0")
                ))
                .resultRatesInPercent(true)
                .resultScale(2)
                .build());
        String expected = """
                 period; startVal; endVal; periodFlow; periodTwr; cumulTwr; annTwr; periodMwr; cumulMwr; annMwr
                2023-01;        0;      0;          0;      0.00;     0.00;   0.00;      0.00;     0.00;   0.00
                2023-02;        0;      0;          0;      0.00;     0.00;   0.00;      0.00;     0.00;   0.00
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

    @Test
    void analyzePerformance4() {
        List<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2023-02-28"))
                .assetValues(List.of(
                        new DateAmount("2022-12-31", "0"),
                        new DateAmount("2023-01-31", "1000"),
                        new DateAmount("2023-02-28", "0")
                ))
                .flows(List.of(
                        new DateAmount("2023-01-01", "500")
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .resultRatesInPercent(true)
                .resultScale(2)
                .build());
        String expected = """
                 period; startVal; endVal; periodFlow; periodTwr; cumulTwr;  annTwr; periodMwr; cumulMwr;  annMwr
                2023-01;        0;   1000;        500;    100.00;   100.00;  100.00;    100.00;   100.00;  100.00
                2023-02;     1000;      0;          0;   -100.00;  -100.00; -100.00;   -100.00;  -100.00; -100.00
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }


    private static String perfAnalysesToGridString(List<PerfAnalysis> perfAnalyses) {
        return collectionToGridString(perfAnalyses,
                List.of(
                        "period",
                        "startVal",
                        "endVal",
                        "periodFlow",
                        "periodTwr",
                        "cumulTwr",
                        "annTwr",
                        "periodMwr",
                        "cumulMwr",
                        "annMwr"
                ),
                List.of(
                        PerfAnalysis::periodCaption,
                        PerfAnalysis::periodStartAssetValueExcl,
                        PerfAnalysis::periodEndAssetValueIncl,
                        PerfAnalysis::periodFlow,
                        PerfAnalysis::periodTwr,
                        PerfAnalysis::cumulativeTwr,
                        PerfAnalysis::annualizedTwr,
                        PerfAnalysis::periodMwr,
                        PerfAnalysis::cumulativeMwr,
                        PerfAnalysis::annualizedMwr
                )
        );
    }

}
