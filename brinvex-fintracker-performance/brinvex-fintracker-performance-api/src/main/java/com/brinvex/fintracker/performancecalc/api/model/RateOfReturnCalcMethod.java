package com.brinvex.fintracker.performancecalc.api.model;

public sealed interface RateOfReturnCalcMethod {

    enum MwrCalcMethod implements RateOfReturnCalcMethod {
        MODIFIED_DIETZ,
        XIRR
    }

    enum TwrCalcMethod implements RateOfReturnCalcMethod {

        TRUE_TWR,

        /**
         * <a href="https://en.wikipedia.org/wiki/Modified_Dietz_method#Linked_return_versus_true_time-weighted_return">
         * https://en.wikipedia.org/wiki/Modified_Dietz_method#Linked_return_versus_true_time-weighted_return</a>
         */
        LINKED_MODIFIED_DIETZ
    }
}
