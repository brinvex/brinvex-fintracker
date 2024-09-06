package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.exception.NotYetImplementedException;
import com.brinvex.fintracker.performancecalc.api.model.MwrCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.TwrCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;

import java.math.BigDecimal;

public class PerformanceCalculatorImpl implements PerformanceCalculator {

    @Override
    public BigDecimal calculateMwr(MwrCalcRequest mwrCalcRequest) {
        return ModifiedDietzCalculator.calculate(mwrCalcRequest);
    }

    @Override
    public BigDecimal calculateTwr(TwrCalcRequest twrCalcRequest) {
        throw new NotYetImplementedException("" + twrCalcRequest);
    }
}
