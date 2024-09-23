package com.brinvex.fintracker.performancecalc.api.service;

import com.brinvex.fintracker.performancecalc.api.model.PerfCalcRequest;

import java.math.BigDecimal;

public interface PerformanceCalculator {

    interface TwrCalculator extends PerformanceCalculator {
    }

    interface TrueTwrCalculator extends TwrCalculator {
    }

    /**
     * <a href="https://en.wikipedia.org/wiki/Modified_Dietz_method#Linked_return_versus_true_time-weighted_return">
     * https://en.wikipedia.org/wiki/Modified_Dietz_method#Linked_return_versus_true_time-weighted_return</a>
     */
    interface LinkedModifiedDietzTwrCalculator extends TwrCalculator {
    }

    interface MwrCalculator extends PerformanceCalculator {
    }

    interface ModifiedDietzMwrCalculator extends MwrCalculator {
    }

    interface SimpleReturnCalculator extends PerformanceCalculator {
    }

    BigDecimal calculateReturn(PerfCalcRequest perfCalcRequest);

}
