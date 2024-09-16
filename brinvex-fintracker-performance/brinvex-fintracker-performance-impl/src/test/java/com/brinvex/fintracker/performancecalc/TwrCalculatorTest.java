package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceCalculatorImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.TWR_TRUE;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TwrCalculatorTest {

    private final PerformanceCalculator perfCalculator = new PerformanceCalculatorImpl();

    @Test
    void twr_readmeExample() {
        FinTracker finTracker = FinTracker.newInstance();
        PerformanceCalculator perfCalculator = finTracker.get(PerformanceModule.class).performanceCalculator();
        RateOfReturnCalcRequest twrCalcReq = RateOfReturnCalcRequest.builder()
                .calcMethod(TWR_TRUE)
                .periodStartDateIncl(parse("2020-06-01"))
                .periodEndDateIncl(parse("2020-06-30"))
                .startValueExcl(new BigDecimal("100000"))
                .endValueIncl(new BigDecimal("135000"))
                .cashFlows(List.of(
                        new DateAmount(parse("2020-06-06"), new BigDecimal("-2000")),
                        new DateAmount(parse("2020-06-11"), new BigDecimal("20000"))
                ))
                .assetValues(List.of(
                        new DateAmount(parse("2020-06-05"), new BigDecimal("101000")),
                        new DateAmount(parse("2020-06-10"), new BigDecimal("132000"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .annualize(false)
                .build();
        BigDecimal twrReturn = perfCalculator.calculateRateOfReturn(twrCalcReq);
        assertEquals("19.6053", twrReturn.multiply(new BigDecimal(100)).setScale(4, HALF_UP).toString());
    }

    @Test
    void twr1() {
        RateOfReturnCalcRequest twrReq1 = RateOfReturnCalcRequest.builder()
                .calcMethod(TWR_TRUE)
                .periodStartDateIncl(parse("2023-01-22"))
                .periodEndDateIncl(parse("2023-01-23"))
                .startValueExcl(new BigDecimal("1000.00"))
                .endValueIncl(new BigDecimal("2000.00"))
                .assetValues(emptyList())
                .cashFlows(emptyList())
                .flowTiming(BEGINNING_OF_DAY)
                .annualize(false)
                .build();
        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(twrReq1);
        assertEquals("1.00", ret1.setScale(2, HALF_UP).toString());
    }

    @Test
    void twr2() {
        RateOfReturnCalcRequest twrReq1 = RateOfReturnCalcRequest.builder()
                .calcMethod(TWR_TRUE)
                .periodStartDateIncl(parse("2023-01-01"))
                .periodEndDateIncl(parse("2024-12-31"))
                .startValueExcl(new BigDecimal("1000.00"))
                .endValueIncl(new BigDecimal("2000.00"))
                .assetValues(emptyList())
                .cashFlows(emptyList())
                .flowTiming(BEGINNING_OF_DAY)
                .annualize(false)
                .build();
        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(twrReq1);
        assertEquals("1.00", ret1.setScale(2, HALF_UP).toString());

        RateOfReturnCalcRequest twrReq2 = twrReq1.toBuilder()
                .flowTiming(FlowTiming.END_OF_DAY)
                .build();
        BigDecimal ret2 = perfCalculator.calculateRateOfReturn(twrReq2);
        assertEquals("1.00", ret2.setScale(2, HALF_UP).toString());
        assertEquals(0, ret2.compareTo(ret1));

        RateOfReturnCalcRequest twrReq3 = twrReq2.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("1000"))))
                .build();
        BigDecimal ret3 = perfCalculator.calculateRateOfReturn(twrReq3);
        assertEquals("0.00", ret3.setScale(2, HALF_UP).toString());

        RateOfReturnCalcRequest twrReq4 = twrReq3.toBuilder()
                .flowTiming(FlowTiming.END_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2024-12-31"), new BigDecimal("2000"))))
                .cashFlows(List.of(new DateAmount(parse("2024-12-31"), new BigDecimal("1000"))))
                .build();
        BigDecimal ret4 = perfCalculator.calculateRateOfReturn(twrReq4);
        assertEquals("0.00", ret4.setScale(2, HALF_UP).toString());
        assertEquals(0, ret4.compareTo(ret3));

        RateOfReturnCalcRequest twrReq5 = twrReq4.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("0"))))
                .build();
        BigDecimal ret5 = perfCalculator.calculateRateOfReturn(twrReq5);
        assertEquals("1.00", ret5.setScale(2, HALF_UP).toString());

        RateOfReturnCalcRequest twrReq6 = twrReq5.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("0"))))
                .build();
        BigDecimal ret6 = perfCalculator.calculateRateOfReturn(twrReq6);
        assertEquals("1.00", ret6.setScale(2, HALF_UP).toString());

        RateOfReturnCalcRequest twrReq7 = twrReq6.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("0.01"))))
                .build();
        BigDecimal ret7 = perfCalculator.calculateRateOfReturn(twrReq7);
        assertEquals("0.999995", ret7.setScale(6, HALF_UP).toString());

        RateOfReturnCalcRequest twrReq8 = twrReq7.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))))
                .build();
        BigDecimal ret8 = perfCalculator.calculateRateOfReturn(twrReq8);
        assertEquals("0.333333", ret8.setScale(6, HALF_UP).toString());

        RateOfReturnCalcRequest twrReq9 = twrReq8.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("4000.01"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))))
                .build();
        BigDecimal ret9 = perfCalculator.calculateRateOfReturn(twrReq9);
        assertEquals("0.333334", ret9.setScale(6, HALF_UP).toString());
        assertEquals(1, ret9.compareTo(ret8));

        RateOfReturnCalcRequest twrReq10 = twrReq9.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("3999.99"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("2000"))))
                .build();
        BigDecimal ret10 = perfCalculator.calculateRateOfReturn(twrReq10);
        assertEquals("0.333332", ret10.setScale(6, HALF_UP).toString());
        assertEquals(-1, ret10.compareTo(ret8));

        RateOfReturnCalcRequest twrReq11 = twrReq10.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .startValueExcl(BigDecimal.ZERO)
                .endValueIncl(new BigDecimal("2000"))
                .assetValues(List.of())
                .cashFlows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("1000"))
                ))
                .build();
        BigDecimal ret11 = perfCalculator.calculateRateOfReturn(twrReq11);
        assertEquals("1.000000", ret11.setScale(6, HALF_UP).toString());
        assertEquals(0, ret11.compareTo(ret1));

        RateOfReturnCalcRequest twrReq12 = twrReq10.toBuilder()
                .flowTiming(BEGINNING_OF_DAY)
                .startValueExcl(BigDecimal.ZERO)
                .endValueIncl(new BigDecimal("2000"))
                .assetValues(List.of())
                .cashFlows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("1000"))
                ))
                .build();
        BigDecimal ret12 = perfCalculator.calculateRateOfReturn(twrReq12);
        assertEquals("1.000000", ret12.setScale(6, HALF_UP).toString());
        assertEquals(0, ret12.compareTo(ret1));

        RateOfReturnCalcRequest twrReq13 = twrReq10.toBuilder()
                .flowTiming(FlowTiming.END_OF_DAY)
                .startValueExcl(new BigDecimal("1000"))
                .endValueIncl(BigDecimal.ZERO)
                .assetValues(List.of())
                .cashFlows(List.of(
                        new DateAmount(parse("2024-12-31"), new BigDecimal("-2000"))
                ))
                .build();
        BigDecimal ret13 = perfCalculator.calculateRateOfReturn(twrReq13);
        assertEquals("1.000000", ret13.setScale(6, HALF_UP).toString());
        assertEquals(0, ret13.compareTo(ret1));

    }

    /*
     * https://en.wikipedia.org/wiki/Time-weighted_return#Example_1
     */
    @Test
    void twr_Wikipedia1() {
        RateOfReturnCalcRequest twrReq1 = RateOfReturnCalcRequest.builder()
                .calcMethod(TWR_TRUE)
                .periodStartDateIncl(parse("2023-01-01"))
                .periodEndDateIncl(parse("2024-12-31"))
                .startValueExcl(new BigDecimal("500.00"))
                .endValueIncl(new BigDecimal("1500.00"))
                .assetValues(List.of(new DateAmount(parse("2023-12-31"), new BigDecimal("1000"))))
                .cashFlows(List.of(new DateAmount(parse("2024-01-01"), new BigDecimal("1000"))))
                .flowTiming(BEGINNING_OF_DAY)
                .annualize(false)
                .build();
        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(twrReq1);
        assertEquals("0.50", ret1.setScale(2, HALF_UP).toString());
    }

}
