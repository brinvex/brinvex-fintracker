/*
package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.PeriodUnit;
import com.brinvex.fintracker.performancecalc.api.model.TwrDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

public class TwrCalculatorImpl {

    public List<TwrDetail> calculateTwr(
            Map<LocalDate, BigDecimal> assetValues,
            Map<LocalDate, BigDecimal> flows,
            PeriodUnit calcPeriodGranularity,
            PeriodUnit resultPeriodGranularity
    ) {
        if (assetValues.size() < 2) {
            throw new IllegalArgumentException("assetValues must contain at least two values");
        }
        SortedMap<LocalDate, BigDecimal> sortedAssetValues = assetValues instanceof SortedMap<LocalDate, BigDecimal> ?
                (SortedMap<LocalDate, BigDecimal>) assetValues : new TreeMap<>(assetValues);
        SortedMap<LocalDate, BigDecimal> sortedFlows = flows instanceof SortedMap<LocalDate, BigDecimal> ?
                (SortedMap<LocalDate, BigDecimal>) flows : new TreeMap<>(flows);


        Period calcPeriod = calcPeriodGranularity.period();
        LocalDate startDateExcl = calcPeriodGranularity.startDate(sortedAssetValues.firstKey());
        LocalDate startDateIncl = startDateExcl.plus(calcPeriod);
        LocalDate endDateIncl = calcPeriodGranularity.startDate(sortedAssetValues.lastKey());

        BigDecimal periodStartValueExcl = sortedAssetValues.headMap(startDateIncl).lastEntry().getValue();

        List<TwrDetail> results = new ArrayList<>();
        int idx = 0;
        BigDecimal cumGrowthFactor = ONE;
        for (LocalDate periodStartIncl = startDateIncl; !periodStartIncl.isAfter(endDateIncl); periodStartIncl = periodStartIncl.plus(calcPeriod)) {
            LocalDate periodEndExcl = periodStartIncl.plus(calcPeriod);
            LocalDate periodEndIncl = periodEndExcl.minusDays(1);
            sortedAssetValues = sortedAssetValues.tailMap(periodStartIncl);
            sortedFlows = sortedFlows.tailMap(periodStartIncl);

            Map.Entry<LocalDate, BigDecimal> periodLastAssetValueEntry = sortedAssetValues.headMap(periodEndExcl).lastEntry();
            BigDecimal periodFlow = sortedFlows.headMap(periodEndExcl).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal periodEndValueIncl = periodLastAssetValueEntry == null ? periodStartValueExcl : periodLastAssetValueEntry.getValue();

            BigDecimal periodStartValueExclWithFlow = periodStartValueExcl.add(periodFlow);

            BigDecimal gain = periodEndValueIncl.subtract(periodStartValueExclWithFlow);

            BigDecimal periodGrowthFactor = gain.compareTo(ZERO) == 0 ? ONE : periodEndValueIncl.divide(periodStartValueExclWithFlow, 4, RoundingMode.HALF_UP);

            cumGrowthFactor = cumGrowthFactor.multiply(periodGrowthFactor);

            results.add(new TwrDetail(
                    idx++,
                    periodStartIncl,
                    periodEndIncl,
                    periodStartValueExcl,
                    periodEndValueIncl,
                    periodFlow,
                    gain,
                    periodGrowthFactor,
                    cumGrowthFactor,
                    BigDecimal.ONE,
                    null,
                    null
            ));

            periodStartValueExcl = periodLastAssetValueEntry == null ? periodStartValueExcl : periodEndValueIncl;
        }


        return results;
    }
}
*/
