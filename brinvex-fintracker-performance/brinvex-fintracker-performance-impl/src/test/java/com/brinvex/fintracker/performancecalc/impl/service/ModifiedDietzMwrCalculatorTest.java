package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.exception.CalculationException;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.FlowTiming;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest.PerfCalcRequestBuilder;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.ModifiedDietzMwrCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifiedDietzMwrCalculatorTest {

    private final static FinTracker finTracker = FinTracker.newInstance();

    private final static PerformanceModule perfModule = finTracker.get(PerformanceModule.class);

    private final static ModifiedDietzMwrCalculator modifiedDietzMwrCalculator = perfModule.modifiedDietzMwrCalculator();

    @Test
    void dietz_readmeExample() {
        ModifiedDietzMwrCalculator mwrCalculator = finTracker.get(PerformanceModule.class)
                .modifiedDietzMwrCalculator();
        BigDecimal mwrReturn = mwrCalculator.calculateReturn(PerfCalcRequest.builder()
                .startDateIncl(parse("2020-06-01"))
                .endDateIncl(parse("2020-06-30"))
                .startAssetValueExcl(new BigDecimal("100000"))
                .endAssetValueIncl(new BigDecimal("135000"))
                .flows(List.of(
                        new DateAmount("2020-06-06", "-2000"),
                        new DateAmount("2020-06-11", "20000")))
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .resultInPercent(true)
                .resultScale(4)
                .build());
        assertEquals("15.2239", mwrReturn.toPlainString());

    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietz_Gips1() {
        PerfCalcRequestBuilder calcReqBuilder = PerfCalcRequest.builder()
                .startDateIncl(parse("2020-06-01"))
                .endDateIncl(parse("2020-06-30"))
                .startAssetValueExcl(new BigDecimal("100000"))
                .endAssetValueIncl(new BigDecimal("135000"))
                .flows(List.of(
                        new DateAmount(parse("2020-06-06"), new BigDecimal("-2000")),
                        new DateAmount(parse("2020-06-11"), new BigDecimal("20000"))
                ))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .resultScale(4)
                .resultInPercent(true);

        BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(calcReqBuilder.build());
        assertEquals("15.3061", ret1.toPlainString());

        PerfCalcRequestBuilder mwrReq2 = calcReqBuilder.copy()
                .flowTiming(BEGINNING_OF_DAY);
        BigDecimal ret2 = modifiedDietzMwrCalculator.calculateReturn(mwrReq2.build());
        assertEquals("15.2239", ret2.toPlainString());

        assertTrue(ret1.compareTo(ret2) > 0);
    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietz_Gips2() {
        PerfCalcRequestBuilder mwrReq = PerfCalcRequest.builder()
                .startDateIncl(parse("2020-06-01"))
                .endDateIncl(parse("2020-06-11"))
                .startAssetValueExcl(new BigDecimal("100000"))
                .endAssetValueIncl(new BigDecimal("125000"))
                .flows(List.of(
                        new DateAmount(parse("2020-06-06"), new BigDecimal("-2000")),
                        new DateAmount(parse("2020-06-11"), new BigDecimal("20000"))
                ))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .resultScale(4)
                .resultInPercent(true);

        BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy().build());
        assertEquals("7.0642", ret1.toPlainString());

        BigDecimal ret2 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .build());
        assertEquals("6.9495", ret2.toPlainString());

        assertTrue(ret1.compareTo(ret2) > 0);
    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.21
     */
    @Test
    void dietz_Gips3() {
        PerfCalcRequestBuilder mwrReq1 = PerfCalcRequest.builder()
                .startDateIncl(parse("2020-06-12"))
                .endDateIncl(parse("2020-06-30"))
                .startAssetValueExcl(new BigDecimal("125000"))
                .endAssetValueIncl(new BigDecimal("135000"))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(mwrReq1.copy().build());
        assertEquals("0.080000", ret1.toPlainString());

        BigDecimal ret2 = modifiedDietzMwrCalculator.calculateReturn(mwrReq1.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .build());
        assertEquals("0.080000", ret2.toPlainString());

        assertEquals(0, ret1.compareTo(ret2));

    }

    /*
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_handbook_for_asset_owners.pdf
     * https://www.gipsstandards.org/wp-content/uploads/2021/03/gips_standards_asset_owner_explanation_of_provisions_section_22_calculations.xlsm
     * Provision 22.A.23
     */
    @Test
    void dietz_Gips4() {
        PerfCalcRequestBuilder mwrReq = PerfCalcRequest.builder()
                .startDateIncl(parse("2017-01-01"))
                .endDateIncl(parse("2020-12-31"))
                .startAssetValueExcl(new BigDecimal("2000000"))
                .endAssetValueIncl(new BigDecimal("2300000"))
                .flows(List.of(
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
                .resultScale(10)
                .annualization(DO_NOT_ANNUALIZE);

        BigDecimal cumRet1 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy().build());
        assertEquals("0.0754846147", cumRet1.toPlainString());

        BigDecimal cumRet2 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .build());
        assertEquals("0.0754812024", cumRet2.toPlainString());

        assertTrue(cumRet1.compareTo(cumRet2) > 0);

        BigDecimal annRet3 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy()
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualization(ANNUALIZE_IF_OVER_ONE_YEAR)
                .build());
        assertEquals("0.0183593390", annRet3.toPlainString());

        BigDecimal annRet4 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .annualization(ANNUALIZE_IF_OVER_ONE_YEAR)
                .build());
        assertEquals("0.0183585312", annRet4.toPlainString());

        assertTrue(annRet3.compareTo(annRet4) > 0);
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietz_Wikipedia1() {
        PerfCalcRequestBuilder mwrReq = PerfCalcRequest.builder()
                .startDateIncl(parse("2022-01-01"))
                .endDateIncl(parse("2023-12-31"))
                .startAssetValueExcl(new BigDecimal("100"))
                .endAssetValueIncl(new BigDecimal("300"))
                .flows(List.of(
                        new DateAmount(parse("2023-01-01"), new BigDecimal("50"))
                ))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE);

        BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy().build());
        assertEquals("1.200658", ret1.toPlainString());

        BigDecimal ret2 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.copy()
                .flowTiming(BEGINNING_OF_DAY)
                .build());
        assertEquals("1.200000", ret2.toPlainString());

        assertTrue(ret1.compareTo(ret2) > 0);
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietz_Wikipedia2() {
        {
            PerfCalcRequestBuilder mwrReq = PerfCalcRequest.builder()
                    .startDateIncl(parse("2016-01-01"))
                    .endDateIncl(parse("2016-12-31"))
                    .startAssetValueExcl(new BigDecimal("1800000"))
                    .endAssetValueIncl(new BigDecimal("1818000"))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualization(DO_NOT_ANNUALIZE);

            BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.build());
            assertNotEquals("3.66", ret1.toPlainString());
            assertEquals("0.010000", ret1.toPlainString());
        }
        {
            PerfCalcRequestBuilder mwrReq = PerfCalcRequest.builder()
                    .startDateIncl(parse("2016-01-01"))
                    .endDateIncl(parse("2016-12-31"))
                    .startAssetValueExcl(new BigDecimal("1800000"))
                    .endAssetValueIncl(new BigDecimal("1818000"))
                    .flowTiming(BEGINNING_OF_DAY)
                    .annualization(DO_NOT_ANNUALIZE);

            BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(mwrReq.build());
            assertEquals("0.010000", ret1.toPlainString());
        }
    }

    /*
     * https://en.wikipedia.org/wiki/Modified_Dietz_method
     */
    @Test
    void dietz_Wikipedia4() {
        assertThrows(CalculationException.class, () -> modifiedDietzMwrCalculator.calculateReturn(PerfCalcRequest.builder()
                .startDateIncl(parse("2016-01-01"))
                .endDateIncl(parse("2016-01-01").plusDays(39))
                .startAssetValueExcl(new BigDecimal("1000"))
                .endAssetValueIncl(new BigDecimal("250"))
                .flows(List.of(
                        new DateAmount(parse("2016-01-01").plusDays(4), new BigDecimal("-1200"))))
                .flowTiming(FlowTiming.END_OF_DAY)
                .annualization(DO_NOT_ANNUALIZE)
                .build()));
    }

    /*
     * https://canadianportfoliomanagerblog.com/calculating-your-modified-dietz-rate-of-return/
     */
    @Test
    void dietz_CanadianPortfolioManager() {
        {
            BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(PerfCalcRequest.builder()
                    .startDateIncl(parse("2020-01-01"))
                    .endDateIncl(parse("2020-12-31"))
                    .startAssetValueExcl(new BigDecimal("100000"))
                    .endAssetValueIncl(new BigDecimal("110828"))
                    .flows(List.of())
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualization(DO_NOT_ANNUALIZE)
                    .resultScale(4)
                    .build());
            assertEquals("0.1083", ret1.toPlainString());
        }
        {
            BigDecimal ret2 = modifiedDietzMwrCalculator.calculateReturn(PerfCalcRequest.builder()
                    .startDateIncl(parse("2020-01-01"))
                    .endDateIncl(parse("2020-12-31"))
                    .startAssetValueExcl(new BigDecimal("100000"))
                    .endAssetValueIncl(new BigDecimal("125039"))
                    .flows(List.of(new DateAmount(parse("2020-03-23"), new BigDecimal("10000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualization(DO_NOT_ANNUALIZE)
                    .resultScale(4)
                    .build());
            assertEquals("0.1396", ret2.toPlainString());
        }
        {
            BigDecimal ret3 = modifiedDietzMwrCalculator.calculateReturn(PerfCalcRequest.builder()
                    .startDateIncl(parse("2020-01-01"))
                    .endDateIncl(parse("2020-12-31"))
                    .startAssetValueExcl(new BigDecimal("100000"))
                    .endAssetValueIncl(new BigDecimal("96616"))
                    .flows(List.of(new DateAmount(parse("2020-03-23"), new BigDecimal("-10000"))))
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualization(DO_NOT_ANNUALIZE)
                    .resultScale(4)
                    .build());
            assertEquals("0.0717", ret3.setScale(4, HALF_UP).toPlainString());
        }
    }

    @Test
    void dietz_cornerCases() {
        {
            BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(PerfCalcRequest.builder()
                    .startDateIncl(parse("2020-01-01"))
                    .endDateIncl(parse("2020-01-01"))
                    .startAssetValueExcl(new BigDecimal("100"))
                    .endAssetValueIncl(new BigDecimal("110"))
                    .flows(List.of())
                    .flowTiming(FlowTiming.END_OF_DAY)
                    .annualization(DO_NOT_ANNUALIZE)
                    .build());
            assertEquals("0.100000", ret1.toPlainString());
        }
        {
            BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(PerfCalcRequest.builder()
                    .startDateIncl(parse("2020-01-01"))
                    .endDateIncl(parse("2020-01-01"))
                    .startAssetValueExcl(new BigDecimal("100"))
                    .endAssetValueIncl(new BigDecimal("110"))
                    .flows(List.of())
                    .flowTiming(BEGINNING_OF_DAY)
                    .annualization(DO_NOT_ANNUALIZE)
                    .build());
            assertEquals("0.100000", ret1.toPlainString());
        }
    }

    @Test
    void dietz_flowWeightsIrrelevantIfGainIsZero() {
        PerfCalcRequestBuilder req = PerfCalcRequest.builder()
                .startDateIncl(parse("2021-02-01"))
                .endDateIncl(parse("2021-02-28"))
                .startAssetValueExcl(new BigDecimal("10000"))
                .endAssetValueIncl(new BigDecimal("10100"))
                .flows(List.of(
                        new DateAmount(parse("2021-02-01"), new BigDecimal("100"))))
                .flowTiming(BEGINNING_OF_DAY)
                .resultScale(10)
                .annualization(DO_NOT_ANNUALIZE);

        BigDecimal ret1 = modifiedDietzMwrCalculator.calculateReturn(req.copy().build());
        assertEquals("0.0000000000", ret1.toPlainString());

        BigDecimal ret2 = modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .flows(List.of(
                        new DateAmount(parse("2021-02-15"), new BigDecimal("100"))))
                .build());
        assertEquals("0.0000000000", ret2.toPlainString());
        assertEquals(0, ret1.compareTo(ret2));

        BigDecimal ret3 = modifiedDietzMwrCalculator.calculateReturn(req.copy()
                .flows(List.of(
                        new DateAmount(parse("2021-02-28"), new BigDecimal("100"))))
                .build());
        assertEquals("0.0000000000", ret3.toPlainString());
        assertEquals(0, ret1.compareTo(ret3));
    }
}
