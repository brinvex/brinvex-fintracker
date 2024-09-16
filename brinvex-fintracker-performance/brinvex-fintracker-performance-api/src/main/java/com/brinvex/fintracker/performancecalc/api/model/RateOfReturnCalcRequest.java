package com.brinvex.fintracker.performancecalc.api.model;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

public record RateOfReturnCalcRequest(
        RateOfReturnCalcMethod calcMethod,
        LocalDate periodStartDateIncl,
        LocalDate periodEndDateIncl,
        BigDecimal startValueExcl,
        BigDecimal endValueIncl,
        Collection<DateAmount> cashFlows,
        Collection<DateAmount> assetValues,
        FlowTiming flowTiming,
        boolean annualize
) {

    @Builder(toBuilder = true)
    public RateOfReturnCalcRequest {
        if (calcMethod == null) {
            throw new IllegalArgumentException("calcMethod must not be null");
        }
        if (periodStartDateIncl == null) {
            throw new IllegalArgumentException("periodStartDateIncl must not be null");
        }
        if (periodEndDateIncl == null) {
            throw new IllegalArgumentException("periodEndDateIncl must not be null");
        }
        if (periodStartDateIncl.isAfter(periodEndDateIncl)) {
            throw new IllegalArgumentException("periodStartDateIncl must be before periodEndDateIncl, given: %s, %s"
                    .formatted(periodStartDateIncl, periodEndDateIncl));
        }
        if (startValueExcl == null) {
            startValueExcl = BigDecimal.ZERO;
        }
        if (endValueIncl == null) {
            endValueIncl = BigDecimal.ZERO;
        }
        if (cashFlows == null) {
            cashFlows = Collections.emptyList();
        }
        if (assetValues == null) {
            assetValues = Collections.emptyList();
        }
        if (flowTiming == null) {
            flowTiming = FlowTiming.BEGINNING_OF_DAY;
        }
    }
}
