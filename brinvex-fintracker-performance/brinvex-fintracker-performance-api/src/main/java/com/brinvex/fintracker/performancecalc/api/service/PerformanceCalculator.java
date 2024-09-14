package com.brinvex.fintracker.performancecalc.api.service;

import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcRequest;

import java.math.BigDecimal;

public interface PerformanceCalculator {

    BigDecimal calculateRateOfReturn(RateOfReturnCalcRequest rateOfReturnCalcRequest);

}
