package com.brinvex.fintracker.performancecalc.api.service;

import com.brinvex.fintracker.performancecalc.api.model.MwrCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.TwrCalcRequest;

import java.math.BigDecimal;

public interface PerformanceCalculator {

    BigDecimal calculateMwr(MwrCalcRequest mwrCalcRequest);

    BigDecimal calculateTwr(TwrCalcRequest twrCalcRequest);

}
