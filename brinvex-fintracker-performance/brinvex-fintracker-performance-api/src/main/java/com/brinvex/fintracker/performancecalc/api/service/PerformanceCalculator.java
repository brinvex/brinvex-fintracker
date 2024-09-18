package com.brinvex.fintracker.performancecalc.api.service;

import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;

import java.math.BigDecimal;

public interface PerformanceCalculator {

    BigDecimal calculateReturn(PerfCalcRequest perfCalcRequest);

}
