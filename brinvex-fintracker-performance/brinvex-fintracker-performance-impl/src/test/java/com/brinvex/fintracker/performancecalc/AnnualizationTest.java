package com.brinvex.fintracker.performancecalc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR;
import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeReturn;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnualizationTest {

    @Test
    void annualize() {
        BigDecimal cumRet;
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2021-01-01"), parse("2021-12-31"));
            assertEquals("1.0", cumRet.toPlainString());
        }
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2021-03-01"), parse("2022-02-28"));
            assertEquals("1.0", cumRet.toPlainString());
        }
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2020-02-27"), parse("2021-02-26"));
            assertEquals("1.0", cumRet.toPlainString());
        }
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2020-02-28"), parse("2021-02-27"));
            assertEquals("1.0", cumRet.toPlainString());
        }
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2020-03-01"), parse("2021-02-28"));
            assertEquals("1.0", cumRet.toPlainString());
        }
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2020-02-28"), parse("2021-02-28"));
            assertEquals("0.9962158948735884", cumRet.toPlainString());
        }
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2020-02-29"), parse("2021-02-28"));
            assertEquals("0.9962158948735884", cumRet.toPlainString());
        }
        {
            cumRet = annualizeReturn(ANNUALIZE_IF_OVER_ONE_YEAR, new BigDecimal("1.0"), parse("2020-02-29"), parse("2021-02-27"));
            assertEquals("1.0", cumRet.toPlainString());
        }
    }
}
