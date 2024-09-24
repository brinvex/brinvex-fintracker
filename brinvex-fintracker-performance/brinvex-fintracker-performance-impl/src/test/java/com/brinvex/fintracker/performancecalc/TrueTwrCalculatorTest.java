package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest.PerfCalcRequestBuilder;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TrueTwrCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrueTwrCalculatorTest {

    private final FinTracker finTracker = FinTracker.newInstance();

    private final PerformanceModule perfModule = finTracker.get(PerformanceModule.class);

    private final TrueTwrCalculator trueTwrCalculator = perfModule.trueTwrCalculator();

    @Test
    void twr_readmeExample() {
        TrueTwrCalculator twrCalculator = finTracker.get(PerformanceModule.class)
                .trueTwrCalculator();
        BigDecimal twrReturn = twrCalculator.calculateReturn(PerfCalcRequest.builder()
                .startDateIncl(parse("2020-06-01"))
                .endDateIncl(parse("2020-06-30"))
                .startAssetValueExcl(new BigDecimal("100000"))
                .endAssetValueIncl(new BigDecimal("135000"))
                .flows(List.of(
                        new DateAmount(parse("2020-06-06"), new BigDecimal("-2000")),
                        new DateAmount(parse("2020-06-11"), new BigDecimal("20000"))))
                .assetValues(List.of(
                        new DateAmount(parse("2020-06-05"), new BigDecimal("101000")),
                        new DateAmount(parse("2020-06-10"), new BigDecimal("132000"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .build());
        assertEquals("0.196053", twrReturn.toPlainString());
    }

    @Test
    void twr1() {
        BigDecimal ret1 = trueTwrCalculator.calculateReturn(PerfCalcRequest.builder()
                .startDateIncl(parse("2023-01-22"))
                .endDateIncl(parse("2023-01-23"))
                .startAssetValueExcl(new BigDecimal("1000.00"))
                .endAssetValueIncl(new BigDecimal("2000.00"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .build());
        assertEquals("1.000000", ret1.toPlainString());
    }

    @Test
    void twr2() {
        PerfCalcRequestBuilder calcReqBuilder = PerfCalcRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2024-12-31"))
                .startAssetValueExcl(new BigDecimal("1000.00"))
                .endAssetValueIncl(new BigDecimal("2000.00"))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        assertEquals("1.000000", trueTwrCalculator.calculateReturn(calcReqBuilder.copy().build()).toPlainString());

        assertEquals("1.000000", trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                        .flowTiming(FlowTiming.END_OF_DAY)
                        .build())
                .toPlainString());

        assertEquals("0.000000", trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                        .flowTiming(BEGINNING_OF_DAY)
                        .assetValues(List.of(
                                new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                        .flows(List.of(
                                new DateAmount(parse("2024-01-01"), new BigDecimal("1000"))))
                        .build())
                .toPlainString());

        assertEquals("0.000000", trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                        .flowTiming(FlowTiming.END_OF_DAY)
                        .assetValues(List.of(
                                new DateAmount(parse("2024-12-31"), new BigDecimal("2000"))))
                        .flows(List.of(
                                new DateAmount(parse("2024-12-31"), new BigDecimal("1000"))))
                        .build())
                .toPlainString());

        assertEquals("1.000000", trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                        .flowTiming(BEGINNING_OF_DAY)
                        .assetValues(List.of(
                                new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                        .flows(List.of(
                                new DateAmount(parse("2024-01-01"), new BigDecimal("0"))))
                        .build())
                .toPlainString());

        assertEquals("1.000000", trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                        .flowTiming(BEGINNING_OF_DAY)
                        .assetValues(List.of(
                                new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                        .flows(List.of(
                                new DateAmount(parse("2024-01-01"), new BigDecimal("0"))))
                        .build())
                .toPlainString());

        assertEquals("0.999995", trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                        .flowTiming(BEGINNING_OF_DAY)
                        .assetValues(List.of(
                                new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                        .flows(List.of(
                                new DateAmount(parse("2024-01-01"), new BigDecimal("0.01"))))
                        .build())
                .toPlainString());

        BigDecimal ret8 = trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(
                        new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                .flows(List.of(
                        new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))))
                .build());
        assertEquals("0.333333", ret8.toPlainString());

        BigDecimal ret9 = trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(
                        new DateAmount(parse("2023-12-31"), new BigDecimal("4000.01"))))
                .flows(List.of(
                        new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))))
                .build());
        assertEquals("0.333334", ret9.toPlainString());
        assertEquals(1, ret9.compareTo(ret8));

        BigDecimal ret10 = trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(
                        new DateAmount(parse("2023-12-31"), new BigDecimal("3999.99"))))
                .flows(List.of(
                        new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))))
                .build());
        assertEquals("0.333332", ret10.toPlainString());
        assertEquals(-1, ret10.compareTo(ret8));

        BigDecimal ret11 = trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .startAssetValueExcl(BigDecimal.ZERO)
                .endAssetValueIncl(new BigDecimal("2000"))
                .assetValues(List.of())
                .flows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("1000"))
                ))
                .build());
        assertEquals("1.000000", ret11.toPlainString());

        BigDecimal ret12 = trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .startAssetValueExcl(BigDecimal.ZERO)
                .endAssetValueIncl(new BigDecimal("2000"))
                .assetValues(List.of())
                .flows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("1000"))
                ))
                .build());
        assertEquals("1.000000", ret12.toPlainString());

        BigDecimal ret13 = trueTwrCalculator.calculateReturn(calcReqBuilder.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .flowTiming(FlowTiming.END_OF_DAY)
                .startAssetValueExcl(new BigDecimal("1000"))
                .endAssetValueIncl(BigDecimal.ZERO)
                .assetValues(List.of())
                .flows(List.of(
                        new DateAmount(parse("2024-12-31"), new BigDecimal("-2000"))
                ))
                .build());
        assertEquals("1.000000", ret13.toPlainString());

    }

    /*
     * https://en.wikipedia.org/wiki/Time-weighted_return#Example_1
     */
    @Test
    void twr_Wikipedia1() {
        PerfCalcRequest twrReq1 = PerfCalcRequest.builder()
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2024-12-31"))
                .startAssetValueExcl(new BigDecimal("500.00"))
                .endAssetValueIncl(new BigDecimal("1500.00"))
                .assetValues(List.of(
                        new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                .flows(List.of(
                        new DateAmount(parse("2024-01-01"), new BigDecimal("1000"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .resultScale(2)
                .build();
        assertEquals("0.50", trueTwrCalculator.calculateReturn(twrReq1).toPlainString());
    }

}
