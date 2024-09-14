package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.exception.CalculationException;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceCalculatorImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifiedDietzCalculatorTest {

    private final PerformanceCalculator perfCalculator = new PerformanceCalculatorImpl();

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietzExample_Gips1() {
        RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                .periodStartDateIncl(parse("2020-06-01"))
                .periodEndDateIncl(parse("2020-06-30"))
                .startValueExcl(new BigDecimal("100000"))
                .endValueIncl(new BigDecimal("135000"))
                .cashFlows(List.of(
                        new DateAmount(parse("2020-06-06"), new BigDecimal("-2000")),
                        new DateAmount(parse("2020-06-11"), new BigDecimal("20000"))
                ))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();

        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
        assertEquals("0.1531", ret1.setScale(4, RoundingMode.HALF_UP).toString());

        RateOfReturnCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal ret2 = perfCalculator.calculateRateOfReturn(mwrReq2);
        assertEquals("0.1522", ret2.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(ret1.compareTo(ret2) > 0);
    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietzExample_Gips2() {
        RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                .periodStartDateIncl(parse("2020-06-01"))
                .periodEndDateIncl(parse("2020-06-11"))
                .startValueExcl(new BigDecimal("100000"))
                .endValueIncl(new BigDecimal("125000"))
                .cashFlows(List.of(
                        new DateAmount(parse("2020-06-06"), new BigDecimal("-2000")),
                        new DateAmount(parse("2020-06-11"), new BigDecimal("20000"))
                ))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();

        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
        assertEquals("0.0706", ret1.setScale(4, RoundingMode.HALF_UP).toString());

        RateOfReturnCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal ret2 = perfCalculator.calculateRateOfReturn(mwrReq2);
        assertEquals("0.0695", ret2.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(ret1.compareTo(ret2) > 0);
    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietzExample_Gips3() {
        RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                .periodStartDateIncl(parse("2020-06-12"))
                .periodEndDateIncl(parse("2020-06-30"))
                .startValueExcl(new BigDecimal("125000"))
                .endValueIncl(new BigDecimal("135000"))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();

        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
        assertEquals("0.0800", ret1.setScale(4, RoundingMode.HALF_UP).toString());

        RateOfReturnCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal ret2 = perfCalculator.calculateRateOfReturn(mwrReq2);
        assertEquals("0.0800", ret2.setScale(4, RoundingMode.HALF_UP).toString());

        assertEquals(0, ret1.compareTo(ret2));

    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.23
     */
    @Test
    void dietzExample_Gips4() {
        RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                .periodStartDateIncl(parse("2017-01-01"))
                .periodEndDateIncl(parse("2020-12-31"))
                .startValueExcl(new BigDecimal("2000000"))
                .endValueIncl(new BigDecimal("2300000"))
                .cashFlows(List.of(
                        new DateAmount(parse("2017-01-08"), new BigDecimal("200000")),
                        new DateAmount(parse("2017-12-24"), new BigDecimal("-50000")),
                        new DateAmount(parse("2018-02-20"), new BigDecimal("-200000")),
                        new DateAmount(parse("2018-03-06"), new BigDecimal("150000")),
                        new DateAmount(parse("2018-12-11"), new BigDecimal("-20000")),
                        new DateAmount(parse("2019-06-25"), new BigDecimal("100000")),
                        new DateAmount(parse("2019-07-03"), new BigDecimal("30000")),
                        new DateAmount(parse("2019-08-14"), new BigDecimal("-50000")),
                        new DateAmount(parse("2020-03-21"), new BigDecimal("-200000")),
                        new DateAmount(parse("2020-06-04"), new BigDecimal("80000")),
                        new DateAmount(parse("2020-11-22"), new BigDecimal("-50000")),
                        new DateAmount(parse("2020-12-03"), new BigDecimal("150000"))
                ))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();

        BigDecimal cret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
        assertEquals("0.0755", cret1.setScale(4, RoundingMode.HALF_UP).toString());

        RateOfReturnCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal cret2 = perfCalculator.calculateRateOfReturn(mwrReq2);
        assertEquals("0.0755", cret2.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(cret1.compareTo(cret2) > 0);

        RateOfReturnCalcRequest mwrReq3 = mwrReq2.toBuilder()
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(true)
                .build();
        BigDecimal annRet3 = perfCalculator.calculateRateOfReturn(mwrReq3);
        assertEquals("0.0184", annRet3.setScale(4, RoundingMode.HALF_UP).toString());

        RateOfReturnCalcRequest mwrReq4 = mwrReq3.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .annualize(true)
                .build();
        BigDecimal annRet4 = perfCalculator.calculateRateOfReturn(mwrReq4);
        assertEquals("0.0184", annRet4.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(annRet3.compareTo(annRet4) > 0);
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietzExample_Wikipedia1() {
        RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                .periodStartDateIncl(parse("2022-01-01"))
                .periodEndDateIncl(parse("2023-12-31"))
                .startValueExcl(new BigDecimal("100"))
                .endValueIncl(new BigDecimal("300"))
                .cashFlows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("50"))
                ))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();

        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
        assertEquals("1.20", ret1.setScale(2, RoundingMode.HALF_UP).toString());

        RateOfReturnCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();

        BigDecimal ret2 = perfCalculator.calculateRateOfReturn(mwrReq2);
        assertEquals("1.20", ret2.setScale(2, RoundingMode.HALF_UP).toString());

        assertTrue(ret1.compareTo(ret2) > 0);
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietzExample_Wikipedia2() {
        {
            RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                    .periodStartDateIncl(parse("2016-01-01"))
                    .periodEndDateIncl(parse("2016-12-31"))
                    .startValueExcl(ZERO)
                    .endValueIncl(new BigDecimal("1818000"))
                    .cashFlows(List.of(new DateAmount(parse("2016-12-30"), new BigDecimal("1800000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
            assertNotEquals("3.66", ret1.setScale(2, RoundingMode.HALF_UP).toString());
            assertEquals("0.01", ret1.setScale(2, RoundingMode.HALF_UP).toString());
        }
        {
            RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                    .periodStartDateIncl(parse("2016-01-01"))
                    .periodEndDateIncl(parse("2016-12-31"))
                    .startValueExcl(ZERO)
                    .endValueIncl(new BigDecimal("1818000"))
                    .cashFlows(List.of(new DateAmount(parse("2016-12-31"), new BigDecimal("1800000"))))
                    .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
            assertEquals("0.01", ret1.setScale(2, RoundingMode.HALF_UP).toString());

            BigDecimal ret2 = perfCalculator.calculateRateOfReturn(mwrReq1.toBuilder().endValueIncl(ZERO).cashFlows(emptyList()).build());
            assertEquals(0, ret2.compareTo(ZERO));

            BigDecimal ret3 = perfCalculator.calculateRateOfReturn(mwrReq1.toBuilder().flowTiming(FlowTiming.END_OF_DAY).build());
            assertEquals(0, ret3.compareTo(ZERO));

            BigDecimal ret4 = perfCalculator.calculateRateOfReturn(mwrReq1.toBuilder()
                    .cashFlows(List.of(
                            new DateAmount(parse("2016-01-01"), new BigDecimal("1000000")),
                            new DateAmount(parse("2016-01-01"), new BigDecimal("800000"))))
                    .build());
            assertEquals(0, ret4.compareTo(ret1), () -> "ret1=%s, ret4=%s".formatted(ret1, ret4));

        }
        {
            RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                    .periodStartDateIncl(parse("2016-01-01"))
                    .periodEndDateIncl(parse("2016-12-31"))
                    .startValueExcl(new BigDecimal("1800000"))
                    .endValueIncl(ZERO)
                    .cashFlows(List.of(new DateAmount(parse("2016-01-01"), new BigDecimal("-1818000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
            assertEquals("0.01", ret1.setScale(2, RoundingMode.HALF_UP).toString());

            BigDecimal ret2 = perfCalculator.calculateRateOfReturn(mwrReq1.toBuilder().startValueExcl(ZERO).cashFlows(emptyList()).build());
            assertEquals(0, ret2.compareTo(ZERO));

            BigDecimal ret3 = perfCalculator.calculateRateOfReturn(mwrReq1.toBuilder().flowTiming(FlowTiming.BEGINNING_OF_DAY).build());
            assertEquals(0, ret3.compareTo(ZERO));

            BigDecimal ret4 = perfCalculator.calculateRateOfReturn(mwrReq1.toBuilder()
                    .cashFlows(List.of(
                            new DateAmount(parse("2016-01-01"), new BigDecimal("-1000000")),
                            new DateAmount(parse("2016-01-01"), new BigDecimal("-818000"))))
                    .build());
            assertEquals(0, ret4.compareTo(ret1), () -> "ret1=%s, ret4=%s".formatted(ret1, ret4));
        }
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietzExample_Wikipedia3() {
        RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                .periodStartDateIncl(parse("2016-01-01"))
                .periodEndDateIncl(parse("2016-01-01"))
                .startValueExcl(new BigDecimal("0"))
                .endValueIncl(new BigDecimal("99"))
                .cashFlows(List.of(new DateAmount(parse("2016-01-01"), new BigDecimal("100"))))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();

        BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
        assertEquals("0.00", ret1.setScale(2, RoundingMode.HALF_UP).toString());
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietzExample_Wikipedia4() {
        RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                .periodStartDateIncl(parse("2016-01-01"))
                .periodEndDateIncl(parse("2016-01-01").plusDays(39))
                .startValueExcl(new BigDecimal("1000"))
                .endValueIncl(new BigDecimal("250"))
                .cashFlows(List.of(new DateAmount(parse("2016-01-01").plusDays(4), new BigDecimal("-1200"))))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();
        assertThrows(CalculationException.class, () -> perfCalculator.calculateRateOfReturn(mwrReq1));
    }

    /*
     * https://canadianportfoliomanagerblog.com/calculating-your-modified-dietz-rate-of-return/
     */
    @Test
    void dietzExample_CanadianPortfolioManager() {
        {
            RateOfReturnCalcRequest mwrReq1 = RateOfReturnCalcRequest.builder()
                    .periodStartDateIncl(parse("2020-01-01"))
                    .periodEndDateIncl(parse("2020-12-31"))
                    .startValueExcl(new BigDecimal("100000"))
                    .endValueIncl(new BigDecimal("110828"))
                    .cashFlows(List.of())
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal ret1 = perfCalculator.calculateRateOfReturn(mwrReq1);
            assertEquals("0.1083", ret1.setScale(4, RoundingMode.HALF_UP).toString());
        }
        {
            RateOfReturnCalcRequest mwrReq2 = RateOfReturnCalcRequest.builder()
                    .periodStartDateIncl(parse("2020-01-01"))
                    .periodEndDateIncl(parse("2020-12-31"))
                    .startValueExcl(new BigDecimal("100000"))
                    .endValueIncl(new BigDecimal("125039"))
                    .cashFlows(List.of(new DateAmount(parse("2020-03-23"), new BigDecimal("10000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal ret2 = perfCalculator.calculateRateOfReturn(mwrReq2);
            assertEquals("0.1396", ret2.setScale(4, RoundingMode.HALF_UP).toString());
        }
        {
            RateOfReturnCalcRequest mwrReq3 = RateOfReturnCalcRequest.builder()
                    .periodStartDateIncl(parse("2020-01-01"))
                    .periodEndDateIncl(parse("2020-12-31"))
                    .startValueExcl(new BigDecimal("100000"))
                    .endValueIncl(new BigDecimal("96616"))
                    .cashFlows(List.of(new DateAmount(parse("2020-03-23"), new BigDecimal("-10000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal ret3 = perfCalculator.calculateRateOfReturn(mwrReq3);
            assertEquals("0.0717", ret3.setScale(4, RoundingMode.HALF_UP).toString());
        }
    }
}
