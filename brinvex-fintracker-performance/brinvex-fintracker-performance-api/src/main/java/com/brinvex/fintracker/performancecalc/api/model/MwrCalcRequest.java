package com.brinvex.fintracker.performancecalc.api.model;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

public record MwrCalcRequest(
        LocalDate periodStartDateIncl,
        LocalDate periodEndDateIncl,
        BigDecimal startValueExcl,
        BigDecimal endValueIncl,
        Collection<DateAmount> cashFlows,
        CashFlowTiming cashFlowTiming,
        MwrCalcMethod calcMethod,
        boolean annualize
) {

    public MwrCalcRequest(
            LocalDate periodStartDateIncl,
            LocalDate periodEndDateIncl,
            BigDecimal startValueExcl,
            BigDecimal endValueIncl,
            Collection<DateAmount> cashFlows
    ) {
        this(
                periodStartDateIncl,
                periodEndDateIncl,
                startValueExcl,
                endValueIncl,
                cashFlows,
                null,
                null,
                null
        );
    }

    @Builder(toBuilder = true)
    @SuppressWarnings("SimplifiableConditionalExpression")
    private MwrCalcRequest(
            LocalDate periodStartDateIncl,
            LocalDate periodEndDateIncl,
            BigDecimal startValueExcl,
            BigDecimal endValueIncl,
            Collection<DateAmount> cashFlows,
            CashFlowTiming cashFlowTiming,
            MwrCalcMethod calcMethod,
            Boolean annualize
    ) {
        this(
                periodStartDateIncl,
                periodEndDateIncl,
                startValueExcl,
                endValueIncl,
                cashFlows == null ? Collections.emptyList() : cashFlows,
                cashFlowTiming == null ? CashFlowTiming.BEGINNING_OF_DAY : cashFlowTiming,
                calcMethod == null ? MwrCalcMethod.MODIFIED_DIETZ : calcMethod,
                annualize == null ? true : annualize
        );
    }
}
