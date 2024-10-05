package com.brinvex.fintracker.performancecalc.api.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the performance details for a specific period.
 *
 * @param periodStartDateIncl       The start date of the period, inclusive
 * @param periodEndDateIncl         The end date of the period, inclusive
 * @param periodCaption             The period caption
 * @param periodStartAssetValueExcl The value of the asset at the start of the period, exclusive of any flows
 * @param periodEndAssetValueIncl   The value of the asset at the end of the period, inclusive of any flows
 * @param periodFlow                The total external cash flow during the period (e.g., deposits or withdrawals)
 * @param periodTwr                 Non-Annualized Time-Weighted Return for this sub-period
 * @param cumulativeTwr             Cumulative Time-Weighted Return up to and including this sub-period
 * @param annualizedTwr             Annualized Time-Weighted Return up to and including this sub-period
 * @param periodMwr                 Non-Annualized Money-Weighted Return for this sub-period
 * @param cumulativeMwr             Cumulative Money-Weighted Return up to and including this sub-period
 * @param annualizedMwr             Annualized Money-Weighted Return up to and including this sub-period
 * @param totalContribution         The sum of the initial asset value and all subsequent cash flows up to and including this sub-period.
 *                                  This value does not account for any gains or losses that occurred due to investment performance.
 * @param periodProfit              The profit during the period, calculated as the difference between the ending asset value and the start value,
 *                                  adjusted for any flows within the period.
 * @param totalProfit               The total cumulative profit up to and including this period.
 * @param periodIncome              The income generated during the period, such as dividends or interest.
 * @param trailingAvgProfit1Y       The average profit over the trailing 12-month period.
 * @param trailingAvgFlow1Y         The average cash flow over the trailing 12-month period.
 * @param trailingAvgIncome1Y       The average income over the trailing 12-month period.
 * @param trailingTwr1Y             The trailing Time-Weighted Return over the past 1 year.
 * @param trailingTwr2Y             The trailing Time-Weighted Return over the past 2 years.
 * @param trailingTwr3Y             The trailing Time-Weighted Return over the past 3 years.
 * @param trailingTwr5Y             The trailing Time-Weighted Return over the past 5 years.
 * @param trailingTwr10Y            The trailing Time-Weighted Return over the past 10 years.
 */
@Builder
public record PerfAnalysis(
        LocalDate periodStartDateIncl,
        LocalDate periodEndDateIncl,
        String periodCaption,
        BigDecimal periodStartAssetValueExcl,
        BigDecimal periodEndAssetValueIncl,
        BigDecimal periodFlow,
        BigDecimal periodTwr,
        BigDecimal cumulativeTwr,
        BigDecimal annualizedTwr,
        BigDecimal periodMwr,
        BigDecimal cumulativeMwr,
        BigDecimal annualizedMwr,
        BigDecimal totalContribution,
        BigDecimal periodProfit,
        BigDecimal totalProfit,
        BigDecimal periodIncome,
        BigDecimal trailingAvgProfit1Y,
        BigDecimal trailingAvgFlow1Y,
        BigDecimal trailingAvgIncome1Y,
        BigDecimal trailingTwr1Y,
        BigDecimal trailingTwr2Y,
        BigDecimal trailingTwr3Y,
        BigDecimal trailingTwr5Y,
        BigDecimal trailingTwr10Y
) {
}
