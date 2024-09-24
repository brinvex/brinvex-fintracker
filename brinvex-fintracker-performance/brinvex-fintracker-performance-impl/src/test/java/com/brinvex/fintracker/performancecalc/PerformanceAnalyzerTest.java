package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.test.support.CollectionStringUtil.collectionToGridString;
import static com.brinvex.fintracker.test.support.AssertionUtil.assertEqualsWithMultilineMsg;
import static java.time.LocalDate.parse;

public class PerformanceAnalyzerTest {

    private final FinTracker finTracker = FinTracker.newInstance();

    private final PerformanceModule perfModule = finTracker.get(PerformanceModule.class);

    private final PerformanceAnalyzer perfAnalyzer = perfModule.performanceAnalyzer();

    @Test
    void analyzePerformance1() {
        List<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(PerfAnalysisRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2023-01-31"))
                .assetValues(List.of(
                        new DateAmount(parse("2022-12-31"), new BigDecimal("10000")),
                        new DateAmount(parse("2023-01-31"), new BigDecimal("10500"))
                ))
                .build());
        String expected = """
                prdStartIncl; prdEndIncl; prdStartValExcl; prdEndValIncl; flowSum; periodTwr; cumulTwr;   annTwr; periodMwr; cumulMwr;   annMwr;
                  2023-01-01; 2023-01-31;           10000;         10500;       0;  0.050000; 0.050000; 0.050000;  0.050000; 0.050000; 0.050000;
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
                        new DateAmount(parse("2022-12-31"), new BigDecimal("10000")),
                        new DateAmount(parse("2023-01-31"), new BigDecimal("10500")),
                        new DateAmount(parse("2023-02-28"), new BigDecimal("10500"))
                ))
                .build());
        String expected = """
                prdStartIncl; prdEndIncl; prdStartValExcl; prdEndValIncl; flowSum; periodTwr; cumulTwr;   annTwr; periodMwr; cumulMwr;   annMwr;
                  2023-01-01; 2023-01-31;           10000;         10500;       0;  0.050000; 0.050000; 0.050000;  0.050000; 0.050000; 0.050000;
                  2023-02-01; 2023-02-28;           10500;         10500;       0;  0.000000; 0.050000; 0.050000;  0.000000; 0.050000; 0.050000;
                """;
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }


    private static String perfAnalysesToGridString(List<PerfAnalysis> perfAnalyses) {
        return collectionToGridString(perfAnalyses,
                List.of(
                        "prdStartIncl",
                        "prdEndIncl",
                        "prdStartValExcl",
                        "prdEndValIncl",
                        "flowSum",
                        "periodTwr",
                        "cumulTwr",
                        "annTwr",
                        "periodMwr",
                        "cumulMwr",
                        "annMwr"
                ),
                List.of(
                        PerfAnalysis::periodStartDateIncl,
                        PerfAnalysis::periodEndDateIncl,
                        PerfAnalysis::periodStartAssetValueExcl,
                        PerfAnalysis::periodEndAssetValueIncl,
                        PerfAnalysis::flowSum,
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
