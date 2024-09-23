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
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSortedMap;
import static java.util.stream.Collectors.toMap;

@Getter
@Accessors(fluent = true)
public final class PerfCalcRequest {
    private final LocalDate startDateIncl;
    private final LocalDate endDateIncl;
    private final BigDecimal startAssetValueExcl;
    private final BigDecimal endAssetValueIncl;
    private final SortedMap<LocalDate, BigDecimal> assetValues;
    private final SortedMap<LocalDate, BigDecimal> flows;
    private final FlowTiming flowTiming;
    private final AnnualizationOption annualization;
    private final int calcScale;
    private final int resultScale;
    private final RoundingMode roundingMode;

    private PerfCalcRequest(
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl,
            Collection<Entry<LocalDate, BigDecimal>> assetValues,
            Collection<Entry<LocalDate, BigDecimal>> flows,
            FlowTiming flowTiming,
            AnnualizationOption annualization,
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
        this.flowTiming = flowTiming == null ? FlowTiming.BEGINNING_OF_DAY : flowTiming;
        this.annualization = annualization == null ? AnnualizationOption.DO_NOT_ANNUALIZE : annualization;
        this.calcScale = calcScale == null ? 20 : calcScale;
        this.resultScale = resultScale == null ? 6 : resultScale;
        this.roundingMode = roundingMode == null ? RoundingMode.HALF_UP : roundingMode;

        this.assetValues = sanitizeAssetValues(assetValues, startDateIncl, endDateIncl, startAssetValueExcl, endAssetValueIncl);
        this.flows = sanitizeFlows(flows, startDateIncl, endDateIncl);
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
        private Collection<Entry<LocalDate, BigDecimal>> assetValues;
        @Setter(AccessLevel.NONE)
        private Collection<Entry<LocalDate, BigDecimal>> flows;
        private FlowTiming flowTiming;
        private AnnualizationOption annualization;
        Integer calcScale;
        Integer resultScale;
        RoundingMode roundingMode;

        private PerfCalcRequestBuilder() {
        }

        @Tolerate
        public PerfCalcRequestBuilder assetValues(Collection<DateAmount> assetValues) {
            this.assetValues = assetValues == null ? null : assetValues.stream().map(e -> Map.entry(e.date(), e.amount())).toList();
            return this;
        }

        @Tolerate
        public PerfCalcRequestBuilder assetValues(Map<LocalDate, BigDecimal> assetValues) {
            this.assetValues = assetValues == null ? null : assetValues.entrySet();
            return this;
        }

        @Tolerate
        public PerfCalcRequestBuilder flows(Collection<DateAmount> flows) {
            this.flows = flows == null ? null : flows.stream().map(e -> Map.entry(e.date(), e.amount())).toList();
            return this;
        }

        @Tolerate
        public PerfCalcRequestBuilder flows(Map<LocalDate, BigDecimal> flows) {
            this.flows = flows == null ? null : flows.entrySet();
            return this;
        }

        public PerfCalcRequest build() {
            return new PerfCalcRequest(
                    this.startDateIncl,
                    this.endDateIncl,
                    this.startAssetValueExcl,
                    this.endAssetValueIncl,
                    this.assetValues,
                    this.flows,
                    this.flowTiming,
                    this.annualization,
                    this.calcScale,
                    this.resultScale,
                    this.roundingMode
            );
        }

        public PerfCalcRequestBuilder copy() {
            return new PerfCalcRequestBuilder()
                    .startDateIncl(startDateIncl)
                    .endDateIncl(endDateIncl)
                    .startAssetValueExcl(startAssetValueExcl)
                    .endAssetValueIncl(endAssetValueIncl)
                    .assetValues(assetValues == null ? null : assetValues.stream().collect(toMap(Entry::getKey, Entry::getValue)))
                    .flows(flows == null ? null : flows.stream().collect(toMap(Entry::getKey, Entry::getValue)))
                    .flowTiming(flowTiming)
                    .annualization(annualization)
                    .calcScale(calcScale)
                    .resultScale(resultScale);
        }
    }

    private static SortedMap<LocalDate, BigDecimal> sanitizeAssetValues(
            Collection<Entry<LocalDate, BigDecimal>> assetValues,
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl
    ) {
        if (assetValues == null) {
            assetValues = emptySet();
        }

        LocalDate startDateExcl = startDateIncl.minusDays(1);
        TreeMap<LocalDate, BigDecimal> sortedAssetValues = new TreeMap<>();
        assetValues.forEach(e -> {
            LocalDate date = e.getKey();
            if (!date.isBefore(startDateExcl) && !date.isAfter(endDateIncl)) {
                BigDecimal value = e.getValue();
                BigDecimal old = sortedAssetValues.put(date, value);
                if (old != null) {
                    throw new IllegalArgumentException(
                            ("The assetValues collection must not contain more than one entry for the same date; " +
                             "given: %s, %s, %s")
                                    .formatted(date, old, value));
                }
            }
        });

        BigDecimal assetValueForStartDateExcl = sortedAssetValues.get(startDateExcl);
        if (assetValueForStartDateExcl != null) {
            if (startAssetValueExcl.compareTo(assetValueForStartDateExcl) != 0) {
                throw new IllegalArgumentException(
                        ("If the assetValues collection contains an entry for startDateExcl, " +
                         "it must be equal to the given startAssetValueExcl; " +
                         "given: startDateExcl=%s, assetValueForStartDateExcl=%s, startAssetValueExcl=%s")
                                .formatted(startDateExcl, assetValueForStartDateExcl, startAssetValueExcl));
            }
        } else {
            sortedAssetValues.put(startDateExcl, startAssetValueExcl);
        }

        BigDecimal assetValueForEndDateIncl = sortedAssetValues.get(endDateIncl);
        if (assetValueForEndDateIncl != null) {
            if (endAssetValueIncl.compareTo(assetValueForEndDateIncl) != 0) {
                throw new IllegalArgumentException(
                        ("If the assetValues collection contains an entry for endDateIncl, " +
                         "it must be equal to the given endAssetValueIncl; " +
                         "given: endDateIncl=%s, assetValueForEndDateIncl=%s, endAssetValueIncl=%s")
                                .formatted(endDateIncl, assetValueForEndDateIncl, endAssetValueIncl));
            }
        } else {
            sortedAssetValues.put(endDateIncl, endAssetValueIncl);
        }

        return unmodifiableSortedMap(sortedAssetValues);
    }

    private static SortedMap<LocalDate, BigDecimal> sanitizeFlows(
            Collection<Entry<LocalDate, BigDecimal>> flows,
            LocalDate startDateIncl,
            LocalDate endDateIncl
    ) {
        if (flows == null) {
            flows = emptySet();
        }
        return unmodifiableSortedMap((SortedMap<LocalDate, ? extends BigDecimal>) flows
                .stream()
                .filter(e -> {
                    LocalDate date = e.getKey();
                    return !date.isBefore(startDateIncl) && !date.isAfter(endDateIncl);
                })
                .collect(toMap(Entry::getKey, Entry::getValue, BigDecimal::add, TreeMap::new)));
    }

}
