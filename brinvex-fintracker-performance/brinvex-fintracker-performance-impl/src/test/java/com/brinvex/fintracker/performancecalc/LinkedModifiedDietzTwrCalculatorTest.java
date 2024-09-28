package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.LinkedModifiedDietzTwrCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.DO_NOT_ANNUALIZE;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkedModifiedDietzTwrCalculatorTest {

    private static final FinTracker finTracker = FinTracker.newInstance();

    @Test
    void linkedModifiedDietzTwr_readmeExample() {
        LinkedModifiedDietzTwrCalculator linkedTwrCalculator = finTracker.get(PerformanceModule.class)
                .linkedModifiedDietzTwrCalculator();
        BigDecimal twrReturn = linkedTwrCalculator.calculateReturn(PerfCalcRequest.builder()
                .startDateIncl(parse("2021-01-01"))
                .endDateIncl(parse("2021-03-31"))
                .startAssetValueExcl(new BigDecimal("10000"))
                .endAssetValueIncl(new BigDecimal("10200"))
                .flows(List.of(
                        new DateAmount(parse("2021-02-15"), new BigDecimal("100"))))
                .assetValues(List.of(
                        new DateAmount(parse("2021-01-31"), new BigDecimal("10100")),
                        new DateAmount(parse("2021-02-28"), new BigDecimal("10201"))
                ))
                .flowTiming(BEGINNING_OF_DAY)
                .resultScale(10)
                .annualization(DO_NOT_ANNUALIZE)
                .build());
        assertEquals("0.0100004877", twrReturn.toPlainString());
    }
}
