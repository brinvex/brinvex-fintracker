package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.exception.NotYetImplementedException;
import com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption;
import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.MwrCalcMethod;
import com.brinvex.fintracker.performancecalc.api.model.RateOfReturnCalcMethod.TwrCalcMethod;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.util.java.validation.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PerformanceCalculatorImpl implements PerformanceCalculator {

    @Override
    public BigDecimal calculateReturn(PerfCalcRequest calcReq) {

        BigDecimal cumulReturn;
        if (calcReq.flows().isEmpty()) {
            cumulReturn = SimpleReturnCalculator.calculateSimpleReturn(
                    calcReq.startAssetValueExcl(),
                    calcReq.endAssetValueIncl(),
                    calcReq.startDateIncl(),
                    calcReq.endDateIncl(),
                    calcReq.annualization(),
                    calcReq.calcScale(),
                    calcReq.roundingMode()
            );
        } else {
            cumulReturn = switch (calcReq.calcMethod()) {
                case TwrCalcMethod.TRUE_TWR -> TrueTwrCalculator.calculateTrueTwrReturn(
                        calcReq.startDateIncl(),
                        calcReq.endDateIncl(),
                        calcReq.assetValues(),
                        calcReq.flows(),
                        calcReq.flowTiming(),
                        calcReq.annualization(),
                        calcReq.calcScale(),
                        calcReq.roundingMode()
                );
                case TwrCalcMethod.LINKED_MODIFIED_DIETZ -> LinkedTwrCalculator.calculateLinkedTwrReturn(
                        MwrCalcMethod.MODIFIED_DIETZ,
                        subPeriodCalcReq -> {
                            Assert.isTrue(subPeriodCalcReq.calcMethod() == MwrCalcMethod.MODIFIED_DIETZ);
                            return ModifiedDietzMwrCalculator.calculateMwrReturn(
                                    subPeriodCalcReq.startDateIncl(),
                                    subPeriodCalcReq.endDateIncl(),
                                    subPeriodCalcReq.startAssetValueExcl(),
                                    subPeriodCalcReq.endAssetValueIncl(),
                                    subPeriodCalcReq.flows(),
                                    subPeriodCalcReq.flowTiming(),
                                    AnnualizationOption.DO_NOT_ANNUALIZE,
                                    subPeriodCalcReq.calcScale(),
                                    subPeriodCalcReq.roundingMode()
                            );
                        },
                        calcReq.startDateIncl(),
                        calcReq.endDateIncl(),
                        calcReq.assetValues(),
                        calcReq.flows(),
                        calcReq.flowTiming(),
                        calcReq.annualization(),
                        calcReq.calcScale(),
                        calcReq.roundingMode()
                );
                case MwrCalcMethod.MODIFIED_DIETZ -> ModifiedDietzMwrCalculator.calculateMwrReturn(
                        calcReq.startDateIncl(),
                        calcReq.endDateIncl(),
                        calcReq.startAssetValueExcl(),
                        calcReq.endAssetValueIncl(),
                        calcReq.flows(),
                        calcReq.flowTiming(),
                        calcReq.annualization(),
                        calcReq.calcScale(),
                        calcReq.roundingMode()
                );
                case MwrCalcMethod.XIRR -> throw new NotYetImplementedException("XIRR");
            };
        }

        return cumulReturn.setScale(calcReq.resultScale(), calcReq.roundingMode());
    }

}
