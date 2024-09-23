package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;

import java.math.BigDecimal;

import static com.brinvex.fintracker.performancecalc.impl.service.AnnualizationUtil.annualizeReturn;

public abstract class BaseCalculatorImpl implements PerformanceCalculator {

    @Override
    public final BigDecimal calculateReturn(PerfCalcRequest perfCalcRequest) {

        BigDecimal cumulReturn;
        if (perfCalcRequest.flows().isEmpty()) {
            cumulReturn = SimpleReturnCalculatorImpl.calculateSimpleCumulReturn(
                    perfCalcRequest.startAssetValueExcl(),
                    perfCalcRequest.endAssetValueIncl(),
                    perfCalcRequest.calcScale(),
                    perfCalcRequest.roundingMode()
            );
        } else {
            cumulReturn = calculateCumulativeReturn(perfCalcRequest);
        }

        BigDecimal unscaledAnnReturn = annualizeReturn(
                perfCalcRequest.annualization(),
                cumulReturn,
                perfCalcRequest.startDateIncl(),
                perfCalcRequest.endDateIncl()
        );
        return unscaledAnnReturn.setScale(perfCalcRequest.resultScale(), perfCalcRequest.roundingMode());
    }

    protected abstract BigDecimal calculateCumulativeReturn(PerfCalcRequest calcRequest);
}
