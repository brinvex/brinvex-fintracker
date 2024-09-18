package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest.PerfCalcRequestBuilder;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.TwrCalcMethod;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceCalculatorImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.END_OF_DAY;
import static com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.MwrCalcMethod.MODIFIED_DIETZ;
import static com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.TwrCalcMethod.TRUE_TWR;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerformanceCalculatorTest {

    private final PerformanceCalculator calculator = new PerformanceCalculatorImpl();

    @Test
    void perfCalc1() {
        PerfCalcRequestBuilder mwrReq1 = PerfCalcRequest.builder()
                .calcMethod(MODIFIED_DIETZ)
                .startDateIncl(parse("2020-06-01"))
                .endDateIncl(parse("2020-06-01"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("150"))
                .flows(List.of(
                        new DateAmount(parse("2020-06-01"), new BigDecimal("25"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);
        BigDecimal mwr = calculator.calculateReturn(mwrReq1.build());
        assertEquals("0.20", mwr.setScale(2, HALF_UP).toString());

        BigDecimal twr = calculator.calculateReturn(mwrReq1
                .calcMethod(TwrCalcMethod.TRUE_TWR)
                .build());

        assertEquals(0, mwr.compareTo(twr));

        mwr = calculator.calculateReturn(mwrReq1
                .flowTiming(FlowTiming.END_OF_DAY)
                .build());
        assertEquals("0.25", mwr.setScale(2, HALF_UP).toString());

        twr = calculator.calculateReturn(mwrReq1
                .calcMethod(TwrCalcMethod.TRUE_TWR)
                .flowTiming(FlowTiming.END_OF_DAY)
                .build());

        assertEquals(0, mwr.compareTo(twr));
    }

    @Test
    void perfCalc2() {
        PerfCalcRequestBuilder mwrReq1 = PerfCalcRequest.builder()
                .calcMethod(MODIFIED_DIETZ)
                .startDateIncl(parse("2020-01-01"))
                .endDateIncl(parse("2020-12-31"))
                .startAssetValueExcl(new BigDecimal("0"))
                .endAssetValueIncl(new BigDecimal("0"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("0.000000", calculator.calculateReturn(mwrReq1.copy().build()).toString());
        assertEquals("0.000000", calculator.calculateReturn(mwrReq1.copy().calcMethod(TRUE_TWR).build()).toString());
    }

    @Test
    void perfCalc3() {
        PerfCalcRequestBuilder mwrReq1 = PerfCalcRequest.builder()
                .calcMethod(MODIFIED_DIETZ)
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2022-12-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("0"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("-1.000000", calculator.calculateReturn(mwrReq1.copy().build()).toString());
        assertEquals("-1.000000", calculator.calculateReturn(mwrReq1.copy().calcMethod(TRUE_TWR).build()).toString());
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

        assertEquals("-1.000000", calculator.calculateReturn(req.copy().calcMethod(MODIFIED_DIETZ).build()).toString());
        assertEquals("-1.000000", calculator.calculateReturn(req.copy().calcMethod(TRUE_TWR).build()).toString());
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

        assertEquals("-1.000000", calculator.calculateReturn(req.copy().calcMethod(MODIFIED_DIETZ).build()).toString());
        assertEquals("-1.000000", calculator.calculateReturn(req.copy().calcMethod(TRUE_TWR).build()).toString());
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

        assertEquals("-1.000000", calculator.calculateReturn(req.copy().calcMethod(MODIFIED_DIETZ).build()).toString());
        assertEquals("-1.000000", calculator.calculateReturn(req.copy().calcMethod(TRUE_TWR).build()).toString());
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

        assertEquals("0.020000", calculator.calculateReturn(req.copy()
                .calcMethod(TRUE_TWR).annualization(DO_NOT_ANNUALIZE).build()).toString());
        assertEquals("0.009950", calculator.calculateReturn(req.copy()
                .calcMethod(TRUE_TWR).annualization(ANNUALIZE).build()).toString());
        assertEquals("0.020000", calculator.calculateReturn(req.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(DO_NOT_ANNUALIZE).build()).toString());
        assertEquals("0.009950", calculator.calculateReturn(req.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(ANNUALIZE).build()).toString());
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

        assertEquals("0.020000", calculator.calculateReturn(req1.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(DO_NOT_ANNUALIZE).build()).toString());
        assertEquals("0.009950", calculator.calculateReturn(req1.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(ANNUALIZE).build()).toString());

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

        assertEquals("0.020000", calculator.calculateReturn(req2.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(DO_NOT_ANNUALIZE).build()).toString());
        assertEquals("0.020000", calculator.calculateReturn(req2.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(ANNUALIZE).build()).toString());

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

        assertEquals("0.040400", calculator.calculateReturn(req.copy()
                .calcMethod(TRUE_TWR).annualization(DO_NOT_ANNUALIZE).build()).toString());
        assertEquals("0.020000", calculator.calculateReturn(req.copy()
                .calcMethod(TRUE_TWR).annualization(ANNUALIZE).build()).toString());
        assertEquals("0.040404", calculator.calculateReturn(req.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(DO_NOT_ANNUALIZE).build()).toString());
        assertEquals("0.020002", calculator.calculateReturn(req.copy()
                .calcMethod(MODIFIED_DIETZ).annualization(ANNUALIZE).build()).toString());
    }
}
