package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest.PerfCalcRequestBuilder;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceCalculatorImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.TwrCalcMethod.TRUE_TWR;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrueTwrCalculatorTest {

    private final PerformanceCalculator perfCalculator = new PerformanceCalculatorImpl();

    @Test
    void twr_readmeExample() {
        FinTracker finTracker = FinTracker.newInstance();
        PerformanceCalculator perfCalculator = finTracker.get(PerformanceModule.class).performanceCalculator();
        BigDecimal twrReturn = perfCalculator.calculateReturn(PerfCalcRequest.builder()
                .calcMethod(TRUE_TWR)
                .startDateIncl(parse("2020-06-01"))
                .endDateIncl(parse("2020-06-30"))
                .startAssetValueExcl(new BigDecimal("100000"))
                .endAssetValueIncl(new BigDecimal("135000"))
                .flows(List.of(
                        new DateAmount(parse("2020-06-06"), new BigDecimal("-2000")),
                        new DateAmount(parse("2020-06-11"), new BigDecimal("20000"))
                ))
                .assetValues(List.of(
                        new DateAmount(parse("2020-06-05"), new BigDecimal("101000")),
                        new DateAmount(parse("2020-06-10"), new BigDecimal("132000"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .build());
        assertEquals("0.196053", twrReturn.toString());
    }

    @Test
    void twr1() {
        PerfCalcRequest twrReq1 = PerfCalcRequest.builder()
                .calcMethod(TRUE_TWR)
                .startDateIncl(parse("2023-01-22"))
                .endDateIncl(parse("2023-01-23"))
                .startAssetValueExcl(new BigDecimal("1000.00"))
                .endAssetValueIncl(new BigDecimal("2000.00"))
                .assetValues(emptyList())
                .flows(emptyList())
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .build();
        BigDecimal ret1 = perfCalculator.calculateReturn(twrReq1);
        assertEquals("1.00", ret1.setScale(2, HALF_UP).toString());
    }

    @Test
    void twr2() {
        PerfCalcRequestBuilder twrReq1 = PerfCalcRequest.builder()
                .calcMethod(TRUE_TWR)
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2024-12-31"))
                .startAssetValueExcl(new BigDecimal("1000.00"))
                .endAssetValueIncl(new BigDecimal("2000.00"))
                .assetValues(emptyList())
                .flows(emptyList())
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .resultScale(6);
        BigDecimal ret1 = perfCalculator.calculateReturn(twrReq1.build());
        assertEquals("1.00", ret1.setScale(2, HALF_UP).toString());

        PerfCalcRequestBuilder twrReq2 = twrReq1
                .flowTiming(FlowTiming.END_OF_DAY);
        BigDecimal ret2 = perfCalculator.calculateReturn(twrReq2.build());
        assertEquals("1.00", ret2.setScale(2, HALF_UP).toString());
        assertEquals(0, ret2.compareTo(ret1));

        PerfCalcRequestBuilder twrReq3 = twrReq2
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("1000"))));
        BigDecimal ret3 = perfCalculator.calculateReturn(twrReq3.build());
        assertEquals("0.00", ret3.setScale(2, HALF_UP).toString());

        PerfCalcRequestBuilder twrReq4 = twrReq3
                .flowTiming(FlowTiming.END_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2024-12-31"), new BigDecimal("2000"))))
                .flows(List.of(new DateAmount(parse("2024-12-31"), new BigDecimal("1000"))));
        BigDecimal ret4 = perfCalculator.calculateReturn(twrReq4.build());
        assertEquals("0.00", ret4.setScale(2, HALF_UP).toString());
        assertEquals(0, ret4.compareTo(ret3));

        PerfCalcRequestBuilder twrReq5 = twrReq4
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("0"))));
        BigDecimal ret5 = perfCalculator.calculateReturn(twrReq5.build());
        assertEquals("1.00", ret5.setScale(2, HALF_UP).toString());

        PerfCalcRequestBuilder twrReq6 = twrReq5
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("0"))));
        BigDecimal ret6 = perfCalculator.calculateReturn(twrReq6.build());
        assertEquals("1.00", ret6.setScale(2, HALF_UP).toString());

        PerfCalcRequestBuilder twrReq7 = twrReq6
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("0.01"))));
        BigDecimal ret7 = perfCalculator.calculateReturn(twrReq7.build());
        assertEquals("0.999995", ret7.setScale(6, HALF_UP).toString());

        PerfCalcRequestBuilder twrReq8 = twrReq7
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))));
        BigDecimal ret8 = perfCalculator.calculateReturn(twrReq8.build());
        assertEquals("0.333333", ret8.setScale(6, HALF_UP).toString());

        PerfCalcRequestBuilder twrReq9 = twrReq8
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000.01"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))));
        BigDecimal ret9 = perfCalculator.calculateReturn(twrReq9.build());
        assertEquals("0.333334", ret9.setScale(6, HALF_UP).toString());
        assertEquals(1, ret9.compareTo(ret8));

        PerfCalcRequestBuilder twrReq10 = twrReq9
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("3999.99"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))));
        BigDecimal ret10 = perfCalculator.calculateReturn(twrReq10.build());
        assertEquals("0.333332", ret10.setScale(6, HALF_UP).toString());
        assertEquals(-1, ret10.compareTo(ret8));

        PerfCalcRequestBuilder twrReq11 = twrReq10
                .flowTiming(BEGINNING_OF_DAY)
                .startAssetValueExcl(BigDecimal.ZERO)
                .endAssetValueIncl(new BigDecimal("2000"))
                .assetValues(List.of())
                .flows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("1000"))
                ));
        BigDecimal ret11 = perfCalculator.calculateReturn(twrReq11.build());
        assertEquals("1.000000", ret11.setScale(6, HALF_UP).toString());
        assertEquals(0, ret11.compareTo(ret1));

        PerfCalcRequestBuilder twrReq12 = twrReq10
                .flowTiming(BEGINNING_OF_DAY)
                .startAssetValueExcl(BigDecimal.ZERO)
                .endAssetValueIncl(new BigDecimal("2000"))
                .assetValues(List.of())
                .flows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("1000"))
                ));
        BigDecimal ret12 = perfCalculator.calculateReturn(twrReq12.build());
        assertEquals("1.000000", ret12.setScale(6, HALF_UP).toString());
        assertEquals(0, ret12.compareTo(ret1));

        PerfCalcRequestBuilder twrReq13 = twrReq10
                .flowTiming(FlowTiming.END_OF_DAY)
                .startAssetValueExcl(new BigDecimal("1000"))
                .endAssetValueIncl(BigDecimal.ZERO)
                .assetValues(List.of())
                .flows(List.of(
                        new DateAmount(parse("2024-12-31"), new BigDecimal("-2000"))
                ));
        BigDecimal ret13 = perfCalculator.calculateReturn(twrReq13.build());
        assertEquals("1.000000", ret13.setScale(6, HALF_UP).toString());
        assertEquals(0, ret13.compareTo(ret1));

    }

    /*
     * https://en.wikipedia.org/wiki/Time-weighted_return#Example_1
     */
    @Test
    void twr_Wikipedia1() {
        PerfCalcRequest twrReq1 = PerfCalcRequest.builder()
                .calcMethod(TRUE_TWR)
                .startDateIncl(parse("2023-01-01"))
                .endDateIncl(parse("2024-12-31"))
                .startAssetValueExcl(new BigDecimal("500.00"))
                .endAssetValueIncl(new BigDecimal("1500.00"))
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                .flows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("1000"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .build();
        BigDecimal ret1 = perfCalculator.calculateReturn(twrReq1);
        assertEquals("0.50", ret1.setScale(2, HALF_UP).toString());
    }

}
