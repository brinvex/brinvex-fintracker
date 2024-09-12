package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.MwrCalcRequest;
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
        MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
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

        BigDecimal r1 = perfCalculator.calculateMwr(mwrReq1);
        assertEquals("0.1531", r1.setScale(4, RoundingMode.HALF_UP).toString());

        MwrCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal r2 = perfCalculator.calculateMwr(mwrReq2);
        assertEquals("0.1522", r2.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(r1.compareTo(r2) > 0);
    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietzExample_Gips2() {
        MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
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

        BigDecimal r1 = perfCalculator.calculateMwr(mwrReq1);
        assertEquals("0.0706", r1.setScale(4, RoundingMode.HALF_UP).toString());

        MwrCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal r2 = perfCalculator.calculateMwr(mwrReq2);
        assertEquals("0.0695", r2.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(r1.compareTo(r2) > 0);
    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietzExample_Gips3() {
        MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
                .periodStartDateIncl(parse("2020-06-12"))
                .periodEndDateIncl(parse("2020-06-30"))
                .startValueExcl(new BigDecimal("125000"))
                .endValueIncl(new BigDecimal("135000"))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(false)
                .build();

        BigDecimal r1 = perfCalculator.calculateMwr(mwrReq1);
        assertEquals("0.0800", r1.setScale(4, RoundingMode.HALF_UP).toString());

        MwrCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal r2 = perfCalculator.calculateMwr(mwrReq2);
        assertEquals("0.0800", r2.setScale(4, RoundingMode.HALF_UP).toString());

        assertEquals(0, r1.compareTo(r2));

    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.23
     */
    @Test
    void dietzExample_Gips4() {
        MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
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

        BigDecimal cr1 = perfCalculator.calculateMwr(mwrReq1);
        assertEquals("0.0755", cr1.setScale(4, RoundingMode.HALF_UP).toString());

        MwrCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();
        BigDecimal cr2 = perfCalculator.calculateMwr(mwrReq2);
        assertEquals("0.0755", cr2.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(cr1.compareTo(cr2) > 0);

        MwrCalcRequest mwrReq3 = mwrReq2.toBuilder()
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualize(true)
                .build();
        BigDecimal ar3 = perfCalculator.calculateMwr(mwrReq3);
        assertEquals("0.0184", ar3.setScale(4, RoundingMode.HALF_UP).toString());

        MwrCalcRequest mwrReq4 = mwrReq3.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .annualize(true)
                .build();
        BigDecimal ar4 = perfCalculator.calculateMwr(mwrReq4);
        assertEquals("0.0184", ar4.setScale(4, RoundingMode.HALF_UP).toString());

        assertTrue(ar3.compareTo(ar4) > 0);
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietzExample_Wikipedia1() {
        MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
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

        BigDecimal r1 = perfCalculator.calculateMwr(mwrReq1);
        assertEquals("1.20", r1.setScale(2, RoundingMode.HALF_UP).toString());

        MwrCalcRequest mwrReq2 = mwrReq1.toBuilder()
                .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                .build();

        BigDecimal r2 = perfCalculator.calculateMwr(mwrReq2);
        assertEquals("1.20", r2.setScale(2, RoundingMode.HALF_UP).toString());

        assertTrue(r1.compareTo(r2) > 0);
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietzExample_Wikipedia2() {
        {
            MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
                    .periodStartDateIncl(parse("2016-01-01"))
                    .periodEndDateIncl(parse("2016-12-31"))
                    .startValueExcl(ZERO)
                    .endValueIncl(new BigDecimal("1818000"))
                    .cashFlows(List.of(new DateAmount(parse("2016-12-30"), new BigDecimal("1800000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal r1 = perfCalculator.calculateMwr(mwrReq1);
            assertNotEquals("3.66", r1.setScale(2, RoundingMode.HALF_UP).toString());
            assertEquals("0.01", r1.setScale(2, RoundingMode.HALF_UP).toString());
        }
        {
            MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
                    .periodStartDateIncl(parse("2016-01-01"))
                    .periodEndDateIncl(parse("2016-12-31"))
                    .startValueExcl(ZERO)
                    .endValueIncl(new BigDecimal("1818000"))
                    .cashFlows(List.of(new DateAmount(parse("2016-12-31"), new BigDecimal("1800000"))))
                    .flowTiming(FlowTiming.BEGINNING_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal r1 = perfCalculator.calculateMwr(mwrReq1);
            assertNotEquals("3.66", r1.setScale(2, RoundingMode.HALF_UP).toString());
            assertEquals("0.01", r1.setScale(2, RoundingMode.HALF_UP).toString());

            BigDecimal r2 = perfCalculator.calculateMwr(mwrReq1.toBuilder().endValueIncl(ZERO).cashFlows(emptyList()).build());
            assertEquals(0, r2.compareTo(ZERO));

            BigDecimal r3 = perfCalculator.calculateMwr(mwrReq1.toBuilder().flowTiming(FlowTiming.END_OF_DAY).build());
            assertEquals(0, r3.compareTo(ZERO));
        }
        {
            MwrCalcRequest mwrReq1 = MwrCalcRequest.builder()
                    .periodStartDateIncl(parse("2016-01-01"))
                    .periodEndDateIncl(parse("2016-12-31"))
                    .startValueExcl(new BigDecimal("1800000"))
                    .endValueIncl(ZERO)
                    .cashFlows(List.of(new DateAmount(parse("2016-01-01"), new BigDecimal("-1818000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualize(false)
                    .build();

            BigDecimal r1 = perfCalculator.calculateMwr(mwrReq1);
            assertNotEquals("3.66", r1.setScale(2, RoundingMode.HALF_UP).toString());
            assertEquals("0.01", r1.setScale(2, RoundingMode.HALF_UP).toString());

            BigDecimal r2 = perfCalculator.calculateMwr(mwrReq1.toBuilder().startValueExcl(ZERO).cashFlows(emptyList()).build());
            assertEquals(0, r2.compareTo(ZERO));

            BigDecimal r3 = perfCalculator.calculateMwr(mwrReq1.toBuilder().flowTiming(FlowTiming.BEGINNING_OF_DAY).build());
            assertEquals(0, r3.compareTo(ZERO));
        }

    }
}
