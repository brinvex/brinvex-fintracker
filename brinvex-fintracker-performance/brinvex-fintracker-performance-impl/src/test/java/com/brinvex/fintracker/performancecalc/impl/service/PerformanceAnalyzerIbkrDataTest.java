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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
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

@EnabledIf("testDataExist")
public class PerformanceAnalyzerIbkrDataTest {

    private static final Logger LOG = LoggerFactory.getLogger(PerformanceAnalyzerIbkrDataTest.class);

    private static final ModuleTestSupport moduleTestSupport = new ModuleTestSupport(PerformanceModule.class);

    private static final FinTracker finTracker = moduleTestSupport.finTracker();

    private static final PerformanceAnalyzer perfAnalyzer = finTracker.get(PerformanceModule.class).performanceAnalyzer();

    private static final Dms dms = moduleTestSupport.dmsFactory().getDms("Fintracker-test-dms");

    private static boolean testDataExist() {
        return dms.exists("ptf11", "ptf11_flows.txt") && dms.exists("ptf11", "ptf11_assetValues.txt");
    }

    private static TreeMap<LocalDate, BigDecimal> assetValues;

    private static LocalDate eurFlowsStartDate;
    private static List<DateAmount> eurFlows;

    @BeforeAll
    public static void beforeAll() {

        assetValues = dms.getTextLines("ptf11", "ptf11_assetValues.txt")
                .stream()
                .filter(not(String::isBlank))
                .map(line -> {
                    String[] lineParts = line.split(",");
                    return new DateAmount(lineParts[0], lineParts[1]);
                })
                .collect(toTreeMap(DateAmount::date, DateAmount::amount));


        List<DateAmount> flows = dms.getTextLines("ptf11", "ptf11_flows.txt")
                .stream()
                .filter(not(String::isBlank))
                .map(line -> line.split(","))
                .map(lineParts -> new DateAmount(lineParts[0], lineParts[1]))
                .toList();

        //the last USD flow occurred on Friday 2023-10-13
        eurFlowsStartDate = LocalDate.parse("2023-10-17");
        eurFlows = flows
                .stream()
                .filter(da -> !da.date().isBefore(eurFlowsStartDate))
                .toList();
    }

    private void testPerfAnalysis(LocalDate startDateIncl, LocalDate endDateIncl) {
        PerfAnalysisRequest perfAnalysisReq = PerfAnalysisRequest.builder()
                .analysisStartDateIncl(startDateIncl)
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
        String expectedResultFileName = "ptf11_perfAnalysesResult_%s_%s.txt".formatted(startDateIncl, endDateIncl);
        LOG.debug("expectedResultFileName={}", expectedResultFileName);
        String expected = dms.getTextContent("ptf11", expectedResultFileName);
        String actual = perfAnalysesToGridString(perfAnalyses);
        assertEqualsWithMultilineMsg(expected, actual);
    }

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
    void ptf11_1() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-17"));
    }

    @Test
    void ptf11_2() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-18"));
    }

    @Test
    void ptf11_3() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-19"));
    }

    @Test
    void ptf11_4() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-20"));
    }

    @Test
    void ptf11_5() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-23"));
    }

    @Test
    void ptf11_6() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-24"));
    }

    @Test
    void ptf11_7() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-25"));
    }

    @Test
    void ptf11_8() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-26"));
    }

    @Test
    void ptf11_9() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-27"));
    }

    @Test
    void ptf11_10() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-10-31"));
    }

    @Test
    void ptf11_11() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-11-01"));
    }

    @Test
    void ptf11_12() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-11-30"));
    }

    @Test
    void ptf11_13() {
        testPerfAnalysis(eurFlowsStartDate, parse("2023-12-29"));
    }

    @Test
    void ptf11_14() {
        testPerfAnalysis(eurFlowsStartDate, parse("2024-01-31"));
    }

    @Test
    void ptf11_15() {
        testPerfAnalysis(eurFlowsStartDate, parse("2024-02-29"));
    }

    @Test
    void ptf11_16() {
        testPerfAnalysis(eurFlowsStartDate, parse("2024-04-30"));
    }

    @Test
    void ptf11_17() {
        testPerfAnalysis(eurFlowsStartDate, parse("2024-07-31"));
    }

    @Test
    void ptf11_18() {
        testPerfAnalysis(eurFlowsStartDate, parse("2024-10-18"));
    }

    @Test
    void ptf11_50() {
        testPerfAnalysis(parse("2023-10-17"), parse("2024-10-18"));
    }

    @Test
    void ptf11_51() {
        testPerfAnalysis(parse("2023-10-18"), parse("2024-10-18"));
    }

    @Test
    void ptf11_52() {
        testPerfAnalysis(parse("2023-11-01"), parse("2024-10-18"));
    }

    @Test
    void ptf11_53() {
        testPerfAnalysis(parse("2023-12-01"), parse("2024-10-18"));
    }

    @Test
    void ptf11_54() {
        testPerfAnalysis(parse("2023-12-29"), parse("2024-10-18"));
    }

    //IBKR gives a slightly different cumMwr here, see test-data file
    @Disabled
    @Test
    void ptf11_55() {
        testPerfAnalysis(parse("2024-01-01"), parse("2024-10-18"));
    }

    @Test
    void ptf11_56() {
        testPerfAnalysis(parse("2024-01-02"), parse("2024-10-18"));
    }

    @Test
    void ptf11_58() {
        testPerfAnalysis(parse("2024-02-01"), parse("2024-10-18"));
    }

    @Test
    void ptf11_59() {
        testPerfAnalysis(parse("2024-02-09"), parse("2024-10-18"));
    }

    //IBKR gives a slightly different cumMwr here, see test-data file
    @Disabled
    @Test
    void ptf11_60() {
        testPerfAnalysis(parse("2024-02-12"), parse("2024-10-18"));
    }

    @Test
    void ptf11_61() {
        testPerfAnalysis(parse("2024-02-13"), parse("2024-10-18"));
    }

    @Test
    void ptf11_62() {
        testPerfAnalysis(parse("2024-02-16"), parse("2024-10-18"));
    }

    //IBKR gives a slightly different cumMwr here, see test-data file
    @Disabled
    @Test
    void ptf11_70() {
        testPerfAnalysis(parse("2024-02-19"), parse("2024-10-18"));
    }

    @Test
    void ptf11_80() {
        testPerfAnalysis(parse("2024-02-20"), parse("2024-10-18"));
    }

    @Test
    void ptf11_90() {
        testPerfAnalysis(parse("2024-02-28"), parse("2024-10-18"));
    }

    @Test
    void ptf11_100() {
        testPerfAnalysis(parse("2024-02-29"), parse("2024-10-18"));
    }

    @Test
    void ptf11_110() {
        testPerfAnalysis(parse("2024-03-01"), parse("2024-10-18"));
    }

    //IBKR gives a slightly different cumMwr here, see test-data file
    @Disabled
    @Test
    void ptf11_140() {
        testPerfAnalysis(parse("2024-06-03"), parse("2024-10-18"));
    }

    @Test
    void ptf11_141() {
        testPerfAnalysis(parse("2024-06-04"), parse("2024-10-18"));
    }

}
