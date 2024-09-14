package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.exception.NotYetImplementedException;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;

import java.math.BigDecimal;

public class PerformanceCalculatorImpl implements PerformanceCalculator {

    @Override
    public BigDecimal calculateRateOfReturn(RateOfReturnCalcRequest rateOfReturnCalcRequest) {
        return switch (rateOfReturnCalcRequest.calcMethod()) {
            case MWR_MODIFIED_DIETZ -> ModifiedDietzCalculator.calculate(rateOfReturnCalcRequest);
            case MWR_XIRR -> throw new NotYetImplementedException("MWR_USING_XIRR");
            case TWR_TRUE -> throw new NotYetImplementedException("TWR_TRUE");
            case TWR_LINKED_MODIFIED_DIETZ -> throw new NotYetImplementedException("TWR_USING_LINKED_MODIFIED_DIETZ");
        };
    }
}
