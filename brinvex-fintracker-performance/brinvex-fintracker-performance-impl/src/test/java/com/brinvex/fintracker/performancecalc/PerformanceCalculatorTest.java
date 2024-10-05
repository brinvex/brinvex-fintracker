package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest.PerfCalcRequestBuilder;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.LinkedModifiedDietzTwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.ModifiedDietzMwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TrueTwrCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.END_OF_DAY;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceCalculatorTest {

    private static final FinTracker finTracker = FinTracker.newInstance();

    private static final PerformanceModule perfModule = finTracker.get(PerformanceModule.class);

    private static final TrueTwrCalculator trueTwrCalculator = perfModule.trueTwrCalculator();

    private static final LinkedModifiedDietzTwrCalculator linkedModifiedDietzTwrCalculator = perfModule.linkedModifiedDietzTwrCalculator();

    private static final ModifiedDietzMwrCalculator modifiedDietzMwrCalculator = perfModule.modifiedDietzMwrCalculator();

    @Test
    void perfCalc1() {
        PerfCalcRequestBuilder mwrReq1 = PerfCalcRequest.builder()
                .startDateIncl(parse("2020-06-01"))
                .endDateIncl(parse("2020-06-01"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("150"))
                .flows(List.of(
                        new DateAmount(parse("2020-06-01"), new BigDecimal("25"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);
        BigDecimal mwr = modifiedDietzMwrCalculator.calculateReturn(mwrReq1.build());
        assertEquals("0.20", mwr.setScale(2, HALF_UP).toPlainString());

        BigDecimal twr = trueTwrCalculator.calculateReturn(mwrReq1.build());

        assertEquals(0, mwr.compareTo(twr));

        mwr = modifiedDietzMwrCalculator.calculateReturn(mwrReq1
                .flowTiming(FlowTiming.END_OF_DAY)
                .build());
        assertEquals("0.25", mwr.setScale(2, HALF_UP).toPlainString());

        twr = trueTwrCalculator.calculateReturn(mwrReq1
                .flowTiming(FlowTiming.END_OF_DAY)
                .build());

        assertEquals(0, mwr.compareTo(twr));
    }

    @Test
    void perfCalc2() {
        PerfCalcRequestBuilder mwrReq1 = PerfCalcRequest.builder()
                .startDateIncl(parse("2020-01-01"))
                .endDateIncl(parse("2020-12-31"))
                .startAssetValueExcl(new BigDecimal("0"))
                .endAssetValueIncl(new BigDecimal("0"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("0.000000", modifiedDietzMwrCalculator.calculateReturn(mwrReq1.copy().build()).toPlainString());
        assertEquals("0.000000", trueTwrCalculator.calculateReturn(mwrReq1.copy().build()).toPlainString());
    }

    @Test
    void perfCalc3() {
        PerfCalcRequestBuilder mwrReq1 = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("0"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("-1.000000", modifiedDietzMwrCalculator.calculateReturn(mwrReq1.copy().build()).toPlainString());
        assertEquals("-1.000000", trueTwrCalculator.calculateReturn(mwrReq1.copy().build()).toPlainString());
    }

    @Test
    void perfCalc4_bankruptcy() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("0"))
                .flows(List.of(
                        new DateAmount(parse("2022-01-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-12-31"), new BigDecimal("100"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("-1.000000", modifiedDietzMwrCalculator.calculateReturn(req.copy().build()).toPlainString());
        assertEquals("-1.000000", trueTwrCalculator.calculateReturn(req.copy().build()).toPlainString());
    }

    @Test
    void perfCalc5_bankruptcy() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("0"))
                .flows(List.of(
                        new DateAmount(parse("2022-01-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2022-01-01"), new BigDecimal("100"))))
                .flowTiming(END_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("-1.000000", modifiedDietzMwrCalculator.calculateReturn(req.copy().build()).toPlainString());
        assertEquals("-1.000000", trueTwrCalculator.calculateReturn(req.copy().build()).toPlainString());
    }

    @Test
    void perfCalc6_bankruptcy() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("0"))
                .flowTiming(END_OF_DAY)
                .annualization(ANNUALIZE_IF_OVER_ONE_YEAR);

        assertEquals("-1.000000", modifiedDietzMwrCalculator.calculateReturn(req.copy().build()).toPlainString());
        assertEquals("-1.000000", trueTwrCalculator.calculateReturn(req.copy().build()).toPlainString());
    }

    @Test
    void perfCalc7() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("0"))
                .endAssetValueIncl(new BigDecimal("102"))
                .flows(List.of(
                        new DateAmount(parse("2022-01-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-12-31"), new BigDecimal("0"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("0.020000", trueTwrCalculator.calculateReturn(req.copy()
                .annualization(DO_NOT_ANNUALIZE).build()).toPlainString());
        assertEquals("0.009950", trueTwrCalculator.calculateReturn(req.copy()
                .annualization(ANNUALIZE).build()).toPlainString());
        assertEquals("0.020000", modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .annualization(DO_NOT_ANNUALIZE).build()).toPlainString());
        assertEquals("0.009950", modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .annualization(ANNUALIZE).build()).toPlainString());
    }

    @Test
    void perfCalc8() {
        PerfCalcRequestBuilder req1 = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("0"))
                .endAssetValueIncl(new BigDecimal("102"))
                .flows(List.of(
                        new DateAmount(parse("2022-01-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-12-31"), new BigDecimal("0"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("0.020000", modifiedDietzMwrCalculator.calculateReturn(req1.copy()
                .annualization(DO_NOT_ANNUALIZE).build()).toPlainString());
        assertEquals("0.009950", modifiedDietzMwrCalculator.calculateReturn(req1.copy()
                .annualization(ANNUALIZE).build()).toPlainString());

        PerfCalcRequestBuilder req2 = PerfCalcRequest.builder()
                .startDateIncl(parse("2022-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("0"))
                .endAssetValueIncl(new BigDecimal("102"))
                .flows(List.of(
                        new DateAmount(parse("2022-01-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-12-31"), new BigDecimal("0"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("0.020000", modifiedDietzMwrCalculator.calculateReturn(req2.copy()
                .annualization(DO_NOT_ANNUALIZE).build()).toPlainString());
        assertEquals("0.020000", modifiedDietzMwrCalculator.calculateReturn(req2.copy()
                .annualization(ANNUALIZE).build()).toPlainString());

    }

    @Test
    void perfCalc9() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("102"))
                .flows(List.of(
                        new DateAmount(parse("2022-01-01"), new BigDecimal("-2"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-12-31"), new BigDecimal("102"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("0.040400", trueTwrCalculator.calculateReturn(req.copy()
                .annualization(DO_NOT_ANNUALIZE).build()).toPlainString());
        assertEquals("0.020000", trueTwrCalculator.calculateReturn(req.copy()
                .annualization(ANNUALIZE).build()).toPlainString());
        assertEquals("0.040404", modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .annualization(DO_NOT_ANNUALIZE).build()).toPlainString());
        assertEquals("0.020002", modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .annualization(ANNUALIZE).build()).toPlainString());
    }

    @Test
    void perfCalc10() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2021-01-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("102"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);
        assertEquals("0.020000", linkedModifiedDietzTwrCalculator.calculateReturn(req.copy().build()).toPlainString());
    }

    @Test
    void perfCalc11() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2021-03-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("102"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);
        assertEquals("0.020000", linkedModifiedDietzTwrCalculator.calculateReturn(req.copy().build()).toPlainString());
    }

    @Test
    void perfCalc12() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2021-03-31"))
                .startAssetValueExcl(new BigDecimal("10000"))
                .endAssetValueIncl(new BigDecimal("10200"))
                .flows(List.of(
                        new DateAmount(parse("2021-02-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-01-31"), new BigDecimal("10100")),
                        new DateAmount(parse("2021-02-28"), new BigDecimal("10200"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);
        assertEquals("0.010000", linkedModifiedDietzTwrCalculator.calculateReturn(req.copy().build()).toPlainString());
    }

    @Test
    void perfCalc13() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2021-03-31"))
                .startAssetValueExcl(new BigDecimal("10000"))
                .endAssetValueIncl(new BigDecimal("10098"))
                .flows(List.of(
                        new DateAmount(parse("2021-02-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-01-31"), new BigDecimal("10100")),
                        new DateAmount(parse("2021-02-28"), new BigDecimal("10200"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        BigDecimal ret1 = linkedModifiedDietzTwrCalculator.calculateReturn(req.copy().build());
        assertEquals("-0.000100", ret1.toPlainString());
    }

    @Test
    void perfCalc15() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2021-03-31"))
                .startAssetValueExcl(new BigDecimal("10000"))
                .endAssetValueIncl(new BigDecimal("10200"))
                .flows(List.of(
                        new DateAmount(parse("2021-02-01"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-01-31"), new BigDecimal("10100")),
                        new DateAmount(parse("2021-02-28"), new BigDecimal("10201"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .calcScale(20)
                .resultScale(20)
                .annualization(DO_NOT_ANNUALIZE);

        BigDecimal ret1 = linkedModifiedDietzTwrCalculator.calculateReturn(req.copy().build());
        assertEquals("0.01000000000000000000", ret1.toPlainString());

        BigDecimal ret2 = linkedModifiedDietzTwrCalculator.calculateReturn(req.copy()
                .flows(List.of(
                        new DateAmount(parse("2021-02-15"), new BigDecimal("100"))))
                .build());
        assertEquals("0.01000048773350241428", ret2.toPlainString());
        assertTrue(ret1.compareTo(ret2) < 0);

        BigDecimal ret3 = linkedModifiedDietzTwrCalculator.calculateReturn(req.copy()
                .flows(List.of(
                        new DateAmount(parse("2021-02-28"), new BigDecimal("100"))))
                .build());
        assertEquals("0.01000094495133500625", ret3.toPlainString());
        assertTrue(ret2.compareTo(ret3) < 0, () -> "ret2=%s, ret3=%s".formatted(ret2, ret3));
    }

    @Test
    void perfCalc16() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2023-12-31"))
                .endAssetValueIncl(new BigDecimal("1000"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .startAssetValueExcl(new BigDecimal("2"))
                .flows(List.of(
                        new DateAmount(parse("2023-12-15"), new BigDecimal("800"))))
                .build());
        BigDecimal ret2 = modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .startAssetValueExcl(new BigDecimal("2"))
                .flows(List.of(
                        new DateAmount(parse("2023-12-16"), new BigDecimal("800"))))
                .build());

        BigDecimal ret3 = modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .startAssetValueExcl(new BigDecimal("1"))
                .flows(List.of(
                        new DateAmount(parse("2023-12-15"), new BigDecimal("800"))))
                .build());
        BigDecimal ret4 = modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .startAssetValueExcl(new BigDecimal("1"))
                .flows(List.of(
                        new DateAmount(parse("2023-12-16"), new BigDecimal("800"))))
                .build());

        assertTrue(ret1.compareTo(ret2) < 0);
        assertTrue(ret3.compareTo(ret4) < 0);
        assertTrue(ret1.compareTo(ret3) < 0, () -> "ret1=%s, ret3=%s".formatted(ret1, ret3));
        assertTrue(ret2.compareTo(ret4) < 0, () -> "ret2=%s, ret4=%s".formatted(ret2, ret4));
    }

}
