package com.brinvex.fintracker.performancecalc.api.model;

import com.brinvex.fintracker.core.api.model.general.DateAmount;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptySortedMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSortedMap;
import static java.util.stream.Collectors.toMap;

@Getter
@Accessors(fluent = true)
public final class PerfCalcRequest {
    private final LocalDate startDateIncl;
    private final LocalDate endDateIncl;
    private final BigDecimal startAssetValueExcl;
    private final BigDecimal endAssetValueIncl;
    private final Map<LocalDate, BigDecimal> assetValues;
    private final SortedMap<LocalDate, BigDecimal> flows;
    private final int largeFlowLevelInPercent;
    private final FlowTiming flowTiming;
    private final AnnualizationOption annualization;
    private final boolean resultInPercent;
    private final int calcScale;
    private final int resultScale;
    private final RoundingMode roundingMode;

    private PerfCalcRequest(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl,
            Map<LocalDate, BigDecimal> assetValuesMap,
            Collection<DateAmount> assetValuesCollection,
            Map<LocalDate, BigDecimal> flowsMap,
            Collection<DateAmount> flowsCollection,
            Integer largeFlowLevelInPercent,
            FlowTiming flowTiming,
            AnnualizationOption annualization,
            Boolean resultInPercent,
            Integer calcScale,
            Integer resultScale,
            RoundingMode roundingMode
    ) {
        if (startDateIncl == null) {
            throw new IllegalArgumentException("startDateIncl must not be null");
        }
        if (endDateIncl == null) {
            throw new IllegalArgumentException("endDateIncl must not be null");
        }
        if (startDateIncl.isAfter(endDateIncl)) {
            throw new IllegalArgumentException("startDateIncl must be before endDateIncl, given: %s, %s"
                    .formatted(startDateIncl, endDateIncl));
        }
        if (startAssetValueExcl == null) {
            throw new IllegalArgumentException("startAssetValueExcl must not be null");
        }
        if (startAssetValueExcl.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("startAssetValueExcl must be greater than or equal to zero");
        }
        if (endAssetValueIncl == null) {
            throw new IllegalArgumentException("endAssetValueIncl must not be null");
        }
        if (endAssetValueIncl.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("endAssetValueIncl must be greater than or equal to zero");
        }
        this.startAssetValueExcl = startAssetValueExcl;
        this.endAssetValueIncl = endAssetValueIncl;
        this.startDateIncl = startDateIncl;
        this.endDateIncl = endDateIncl;
        this.largeFlowLevelInPercent = largeFlowLevelInPercent == null ? 5 : largeFlowLevelInPercent;
        this.flowTiming = flowTiming == null ? FlowTiming.BEGINNING_OF_DAY : flowTiming;
        this.annualization = annualization == null ? AnnualizationOption.DO_NOT_ANNUALIZE : annualization;
        this.resultInPercent = resultInPercent != null && resultInPercent;
        this.calcScale = calcScale == null ? 20 : calcScale;
        this.resultScale = resultScale == null ? 6 : resultScale;
        this.roundingMode = roundingMode == null ? RoundingMode.HALF_UP : roundingMode;

        this.assetValues = unmodifiableMap(sanitizeAssetValues(
                assetValuesMap,
                assetValuesCollection,
                startDateIncl,
                endDateIncl
        ));

        this.flows = unmodifiableSortedMap(sanitizeFlows(
                flowsMap,
                flowsCollection,
                startDateIncl,
                endDateIncl
        ));
    }


    public static PerfCalcRequestBuilder builder() {
        return new PerfCalcRequestBuilder();
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class PerfCalcRequestBuilder {

        private LocalDate startDateIncl;
        private LocalDate endDateIncl;
        private BigDecimal startAssetValueExcl;
        private BigDecimal endAssetValueIncl;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> assetValuesMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> assetValuesCollection;
        @Setter(AccessLevel.NONE)
        private Map<LocalDate, BigDecimal> flowsMap;
        @Setter(AccessLevel.NONE)
        private Collection<DateAmount> flowsCollection;
        private Integer largeFlowLevelInPercent;
        private FlowTiming flowTiming;
        private AnnualizationOption annualization;
        Boolean resultInPercent;
        Integer calcScale;
        Integer resultScale;
        RoundingMode roundingMode;

        private PerfCalcRequestBuilder() {
        }

        @Tolerate
        public PerfCalcRequestBuilder assetValues(Map<LocalDate, BigDecimal> assetValues) {
            this.assetValuesCollection = null;
            this.assetValuesMap = assetValues;
            return this;
        }

        @Tolerate
        public PerfCalcRequestBuilder assetValues(Collection<DateAmount> assetValues) {
            this.assetValuesMap = null;
            this.assetValuesCollection = assetValues;
            return this;
        }

        @Tolerate
        public PerfCalcRequestBuilder flows(Map<LocalDate, BigDecimal> flows) {
            this.flowsCollection = null;
            this.flowsMap = flows;
            return this;
        }

        @Tolerate
        public PerfCalcRequestBuilder flows(Collection<DateAmount> flows) {
            this.flowsMap = null;
            this.flowsCollection = flows;
            return this;
        }

        public PerfCalcRequest build() {
            return new PerfCalcRequest(
                    startDateIncl,
                    endDateIncl,
                    startAssetValueExcl,
                    endAssetValueIncl,
                    assetValuesMap,
                    assetValuesCollection,
                    flowsMap,
                    flowsCollection,
                    largeFlowLevelInPercent,
                    flowTiming,
                    annualization,
                    resultInPercent,
                    calcScale,
                    resultScale,
                    roundingMode);
        }

        public PerfCalcRequestBuilder copy() {
            PerfCalcRequestBuilder copy = new PerfCalcRequestBuilder();
            copy.startDateIncl = startDateIncl;
            copy.endDateIncl = endDateIncl;
            copy.startAssetValueExcl = startAssetValueExcl;
            copy.endAssetValueIncl = endAssetValueIncl;
            copy.largeFlowLevelInPercent = largeFlowLevelInPercent;
            copy.flowTiming = flowTiming;
            copy.annualization = annualization;
            copy.resultInPercent = resultInPercent;
            copy.calcScale = calcScale;
            copy.resultScale = resultScale;
            copy.assetValuesMap = assetValuesMap;
            copy.assetValuesCollection = assetValuesCollection;
            copy.flowsMap = flowsMap;
            copy.flowsCollection = flowsCollection;
            return copy;
        }
    }

    static Map<LocalDate, BigDecimal> sanitizeAssetValues(Map<LocalDate, BigDecimal> assetValuesMap, Collection<DateAmount> assetValuesCollection, LocalDate startDateIncl, LocalDate endDateIncl) {
        Map<LocalDate, BigDecimal> sanitizedAssetValues;
        if (assetValuesMap instanceof HashMap) {
            sanitizedAssetValues = assetValuesMap;
        } else {
            sanitizedAssetValues = new HashMap<>();
            if (assetValuesMap != null) {
                sanitizedAssetValues.putAll(assetValuesMap);
            } else if (assetValuesCollection != null) {
                LocalDate startDateExcl = startDateIncl.minusDays(1);
                for (DateAmount dateAssetValue : assetValuesCollection) {
                    LocalDate date = dateAssetValue.date();
                    BigDecimal assetValue = dateAssetValue.amount();
                    if (!date.isBefore(startDateExcl) && !date.isAfter(endDateIncl)) {
                        BigDecimal oldAssetValue = sanitizedAssetValues.put(date, assetValue);
                        if (oldAssetValue != null && oldAssetValue.compareTo(assetValue) != 0) {
                            throw new IllegalArgumentException((
                                    "The assetValues collection must not contain different entries for the same date; " +
                                    "given: %s, %s, %s")
                                    .formatted(date, oldAssetValue, assetValue));
                        }
                    }
                }
            }
        }
        return sanitizedAssetValues;
    }

    static SortedMap<LocalDate, BigDecimal> sanitizeFlows(Map<LocalDate, BigDecimal> flowsMap, Collection<DateAmount> flowsCollection, LocalDate startDateIncl, LocalDate endDateIncl) {
        SortedMap<LocalDate, BigDecimal> sanitizedFlows;
        if (flowsMap != null) {
            if (flowsMap instanceof SortedMap) {
                sanitizedFlows = ((SortedMap<LocalDate, BigDecimal>) flowsMap);
            } else {
                sanitizedFlows = new TreeMap<>(flowsMap);
            }
            if (!sanitizedFlows.isEmpty()) {
                LocalDate firstKey = sanitizedFlows.firstKey();
                LocalDate lastKey = sanitizedFlows.lastKey();
                LocalDate subFirstKey = startDateIncl.isBefore(firstKey) ? firstKey : startDateIncl;
                LocalDate subLastKey = endDateIncl.isAfter(lastKey) ? lastKey : endDateIncl;
                if (subFirstKey.isAfter(subLastKey)) {
                    sanitizedFlows = emptySortedMap();
                } else {
                    sanitizedFlows = sanitizedFlows.subMap(subFirstKey, subLastKey.plusDays(1));
                }
            }
        } else {
            if (flowsCollection != null) {
                sanitizedFlows = flowsCollection
                        .stream()
                        .filter(dateAmount -> !dateAmount.isBefore(startDateIncl) && !dateAmount.isAfter(endDateIncl))
                        .collect(toMap(DateAmount::date, DateAmount::amount, BigDecimal::add, TreeMap::new));
            } else {
                sanitizedFlows = emptySortedMap();
            }
        }
        return sanitizedFlows;
    }

}
