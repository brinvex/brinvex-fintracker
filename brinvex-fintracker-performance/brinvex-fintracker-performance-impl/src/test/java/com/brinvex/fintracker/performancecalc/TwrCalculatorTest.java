/*
package com.brinvex.fintracker.performancecalc;

import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.TwrDetail;
import com.brinvex.fintracker.performancecalc.impl.service.TwrCalculatorImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.parse;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TwrCalculatorTest {

    private final TwrCalculatorImpl twrCalculator = new TwrCalculatorImpl();

    @Test
    void twr_1d_noFlows() {
        List<TwrDetail> twrDetails = twrCalculator.calculateTwr(
                Map.of(
                        parse("2023-01-22"), new BigDecimal("1000.00"),
                        parse("2023-01-23"), new BigDecimal("2000.00")
                ),
                emptyMap(),
                PeriodUnit.DAY,
                PeriodUnit.DAY
        );
        assertEquals(1, twrDetails.size());
        TwrDetail twrDetail = twrDetails.getFirst();
        assertEquals("1000.00", twrDetail.startValueExcl().toString());
        assertEquals("2000.00", twrDetail.endValueIncl().toString());
        assertEquals("1000.00", twrDetail.gain().toString());
        assertEquals("2.00", twrDetail.periodGrowthFactor().setScale(2, HALF_UP).toString());
        assertEquals("2.00", twrDetail.cumGrowthFactor().setScale(2, HALF_UP).toString());
    }

    @Test
    void twr_1y_noFlows() {
        List<TwrDetail> twrDetails = twrCalculator.calculateTwr(
                Map.of(
                        parse("2023-01-22"), new BigDecimal("1000.00"),
                        parse("2024-01-22"), new BigDecimal("2000.00")
                ),
                emptyMap(),
                PeriodUnit.DAY,
                PeriodUnit.DAY
        );
        assertEquals(365, twrDetails.size());
        for (int i = 0, twrDetailsSize = twrDetails.size(); i < twrDetailsSize; i++) {
            TwrDetail twrDetail = twrDetails.get(i);
            assertEquals("1000.00", twrDetail.startValueExcl().toString());
            if (i != twrDetailsSize - 1) {
                assertEquals("1000.00", twrDetail.endValueIncl().toString());
                assertEquals("1.00", twrDetail.periodGrowthFactor().setScale(2, HALF_UP).toString());
                assertEquals("1.00", twrDetail.cumGrowthFactor().setScale(2, HALF_UP).toString());
            } else {
                assertEquals("2000.00", twrDetail.endValueIncl().toString());
                assertEquals("2.00", twrDetail.periodGrowthFactor().setScale(2, HALF_UP).toString());
                assertEquals("2.00", twrDetail.cumGrowthFactor().setScale(2, HALF_UP).toString());
            }
        }
    }
}
*/
