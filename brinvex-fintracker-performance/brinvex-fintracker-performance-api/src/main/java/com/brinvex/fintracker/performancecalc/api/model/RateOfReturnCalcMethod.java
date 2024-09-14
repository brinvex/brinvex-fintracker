package com.brinvex.fintracker.performancecalc.api.model;

public enum RateOfReturnCalcMethod {

    MWR_MODIFIED_DIETZ,

    MWR_XIRR,

    TWR_TRUE,

    /**
     * <a href="https://en.wikipedia.org/wiki/Modified_Dietz_method#Linked_return_versus_true_time-weighted_return">
     *     https://en.wikipedia.org/wiki/Modified_Dietz_method#Linked_return_versus_true_time-weighted_return</a>
     */
    TWR_LINKED_MODIFIED_DIETZ

}
