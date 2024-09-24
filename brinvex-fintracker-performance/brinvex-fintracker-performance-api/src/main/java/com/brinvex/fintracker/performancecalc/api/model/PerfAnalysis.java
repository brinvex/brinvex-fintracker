package com.brinvex.fintracker.performancecalc.api.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the performance details for a specific period.
 *
 * @param periodStartDateIncl The start date of the period, inclusive
 * @param periodEndDateIncl   The end date of the period, inclusive
 * @param periodStartAssetValueExcl The value of the asset at the start of the period, exclusive of any flows
 * @param periodEndAssetValueIncl   The value of the asset at the end of the period, inclusive of any flows
 * @param flowSum                The total cash flowSum during the period (e.g., deposits or withdrawals)
 * @param periodTwr           Non-Annualized Time-Weighted Return for this sub-period
 * @param cumulativeTwr       Cumulative Time-Weighted Return up to and including this sub-period
 * @param annualizedTwr       Annualized Time-Weighted Return up to and including this sub-period
 * @param periodMwr           Non-Annualized Money-Weighted Return for this sub-period
 * @param cumulativeMwr       Cumulative Money-Weighted Return up to and including this sub-period
 * @param annualizedMwr       Annualized Money-Weighted Return up to and including this sub-period
 */
@Builder
public record PerfAnalysis(
        LocalDate periodStartDateIncl,
        LocalDate periodEndDateIncl,
        BigDecimal periodStartAssetValueExcl,
        BigDecimal periodEndAssetValueIncl,
        BigDecimal flowSum,
        BigDecimal periodTwr,
        BigDecimal cumulativeTwr,
        BigDecimal annualizedTwr,
        BigDecimal periodMwr,
        BigDecimal cumulativeMwr,
        BigDecimal annualizedMwr
) {
}

