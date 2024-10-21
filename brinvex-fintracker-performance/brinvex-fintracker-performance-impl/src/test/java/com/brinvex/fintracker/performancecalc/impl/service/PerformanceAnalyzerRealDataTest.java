package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TrueTwrCalculator;
import com.brinvex.fintracker.test.support.ModuleTestSupport;
import com.brinvex.util.dms.api.Dms;
import com.brinvex.util.java.Num;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.SequencedCollection;
import java.util.TreeMap;

import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.END_OF_DAY;
import static com.brinvex.fintracker.test.support.AssertionUtil.assertEqualsWithMultilineMsg;
import static com.brinvex.fintracker.test.support.CollectionStringUtil.collectionToGridString;
import static com.brinvex.util.java.Collectors.toTreeMap;
import static java.time.LocalDate.parse;
import static java.util.function.Predicate.not;

public class PerformanceAnalyzerRealDataTest {

    private static final Logger LOG = LoggerFactory.getLogger(PerformanceAnalyzerRealDataTest.class);

    private static final ModuleTestSupport moduleTestSupport = new ModuleTestSupport(PerformanceModule.class);

    private static final FinTracker finTracker = moduleTestSupport.finTracker();

    private static final PerformanceAnalyzer perfAnalyzer = finTracker.get(PerformanceModule.class).performanceAnalyzer();

    private static final Dms dms = moduleTestSupport.dmsFactory().getDms("Fintracker-test-dms");

    private static String perfAnalysesToGridString(SequencedCollection<PerfAnalysis> perfAnalyses) {
        return collectionToGridString(perfAnalyses,
                List.of(
                        "period",
                        "startVal",
                        "endVal",
                        "prdFlow",
                        "cumTwr",
                        "cumMwr",
                        "prdProf",
                        "totProf"
                ),
                List.of(
                        PerfAnalysis::periodCaption,
                        perfAnalysis -> Num.setScale2(perfAnalysis.periodStartAssetValueExcl()),
                        perfAnalysis1 -> Num.setScale2(perfAnalysis1.periodEndAssetValueIncl()),
                        perfAnalysis2 -> Num.setScale2(perfAnalysis2.periodFlow()),
                        PerfAnalysis::cumulativeTwr,
                        PerfAnalysis::cumulativeMwr,
                        perfAnalysis3 -> Num.setScale2(perfAnalysis3.periodProfit()),
                        perfAnalysis4 -> Num.setScale2(perfAnalysis4.totalProfit())
                )
        );
    }

    @Test
    void ptf11() {
        List<DateAmount> flows = dms.getTextLines("ptf11", "ptf11_flows.txt")
                .stream()
                .filter(not(String::isBlank))
                .map(line -> line.split(","))
                .map(lineParts -> new DateAmount(lineParts[0], lineParts[1]))
                .toList();
        TreeMap<LocalDate, BigDecimal> assetValues = dms.getTextLines("ptf11", "ptf11_assetValues.txt")
                .stream()
                .filter(not(String::isBlank))
                .map(line -> {
                    String[] lineParts = line.split(",");
                    return new DateAmount(lineParts[0], lineParts[1]);
                })
                .collect(toTreeMap(DateAmount::date, DateAmount::amount));

        //the last USD flow occurred on Friday 2023-10-13
        LocalDate eurFlowsStartDate = LocalDate.parse("2023-10-17");

        List<DateAmount> eurFlows = flows
                .stream()
                .filter(da -> !da.date().isBefore(eurFlowsStartDate))
                .toList();

        List<LocalDate> endDates = List.of(
                parse("2023-10-17"),
                parse("2023-10-18"),
                parse("2023-10-19"),
                parse("2023-10-20"),
                parse("2023-10-23"),
                parse("2023-10-24"),
                parse("2023-10-25"),
                parse("2023-10-26"),
                parse("2023-10-27"),
                parse("2023-10-31"),
                parse("2023-11-01"),
                parse("2023-11-30"),
                parse("2023-12-29"),
                parse("2024-01-31"),
                parse("2024-02-29"),
                parse("2024-04-30"),
                parse("2024-07-31"),
                parse("2024-10-18")
        );

        for (LocalDate endDateIncl : endDates) {
            PerfAnalysisRequest perfAnalysisReq = PerfAnalysisRequest.builder()
                    .analysisStartDateIncl(eurFlowsStartDate)
                    .analysisEndDateIncl(endDateIncl)
                    .flows(eurFlows)
                    .assetValues(date -> assetValues.floorEntry(date).getValue())
                    .twrFlowTiming(BEGINNING_OF_DAY)
                    .mwrFlowTiming(END_OF_DAY)
                    .twrCalculatorType(TrueTwrCalculator.class)
                    .mwrCalculatorType(MwrCalculator.class)
                    .calcScale(10)
                    .resultScale(2)
                    .resultRatesInPercent(true)
                    .calculateMwr(true)
                    .build();
            SequencedCollection<PerfAnalysis> perfAnalyses = perfAnalyzer.analyzePerformance(perfAnalysisReq);
            String expectedResultFileName = "ptf11_perfAnalysesResult_%s.txt".formatted(endDateIncl);
            LOG.debug("expectedResultFileName={}", expectedResultFileName);
            String expected = dms.getTextContent("ptf11", expectedResultFileName);
            String actual = perfAnalysesToGridString(perfAnalyses);
            assertEqualsWithMultilineMsg(expected, actual);
        }
    }


}
