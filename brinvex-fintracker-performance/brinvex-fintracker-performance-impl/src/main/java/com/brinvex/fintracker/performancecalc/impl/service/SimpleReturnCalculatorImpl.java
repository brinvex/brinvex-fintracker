package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

public class SimpleReturnCalculatorImpl extends BaseCalculatorImpl implements PerformanceCalculator.SimpleReturnCalculator {

    @Override
    protected BigDecimal calculateCumulativeReturn(PerfCalcRequest calcReq) {
        return calculateSimpleCumulReturn(
                calcReq.startAssetValueExcl(),
                calcReq.endAssetValueIncl(),
                calcReq.calcScale(),
                calcReq.roundingMode());
    }

    protected static BigDecimal calculateSimpleCumulReturn(
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl,
            int calcScale,
            RoundingMode roundingMode
    ) {
        BigDecimal cumulReturn;
        if (startAssetValueExcl.compareTo(ZERO) == 0) {
            if (endAssetValueIncl.compareTo(ZERO) != 0) {
                throw new IllegalArgumentException((
                        "if startAssetValueExcl is zero, then the endAssetValueIncl must be zero, given: %s")
                        .formatted(endAssetValueIncl));
            }
            cumulReturn = ZERO;
        } else if (endAssetValueIncl.compareTo(ZERO) == 0) {
            cumulReturn = ONE.negate();
        } else {
            cumulReturn = endAssetValueIncl.subtract(startAssetValueExcl).divide(startAssetValueExcl, calcScale, roundingMode);
        }

        return cumulReturn;
    }
}
