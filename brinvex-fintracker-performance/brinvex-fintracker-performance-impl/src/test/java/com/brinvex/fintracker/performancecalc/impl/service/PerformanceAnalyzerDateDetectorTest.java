package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.core.api.model.general.DateRange;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.BEGINNING_OF_DAY;
import static com.brinvex.fintracker.performancecalc.api.model.FlowTiming.END_OF_DAY;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerformanceAnalyzerDateDetectorTest {

    @Test
    void detectInvestmentStartDate1() {
        assertEquals(new DateRange.Inclusive("2023-01-01","2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(),
                new TreeSet<>(),
                BEGINNING_OF_DAY
        ));
        assertEquals(new DateRange.Inclusive("2023-01-01","2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(),
                new TreeSet<>(),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate2() {
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2022-12-30"), ONE),
                new TreeSet<>(),
                BEGINNING_OF_DAY
        ));
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2022-12-30"), ONE),
                new TreeSet<>(),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate3() {
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2022-12-31"), ONE),
                new TreeSet<>(),
                BEGINNING_OF_DAY
        ));
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2022-12-31"), ONE),
                new TreeSet<>(),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate4() {
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2022-12-31"), ZERO),
                new TreeSet<>(),
                BEGINNING_OF_DAY
        ));
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2022-12-31"), ZERO),
                new TreeSet<>(),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate5() {
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-01-01"), ONE),
                new TreeSet<>(),
                BEGINNING_OF_DAY
        ));
        assertEquals(new DateRange.Inclusive("2023-01-01", "2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentDateRange(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-01-01"), ONE),
                new TreeSet<>(),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate6() {
        assertEquals(parse("2023-02-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-02-01"), ONE),
                new TreeSet<>(),
                BEGINNING_OF_DAY
        ));
        assertEquals(parse("2023-02-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-02-01"), ONE),
                new TreeSet<>(),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate7() {
        assertEquals(parse("2023-01-01"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-12-01"), ONE),
                new TreeSet<>(),
                BEGINNING_OF_DAY
        ));
        assertEquals(parse("2023-01-01"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-12-01"), ONE),
                new TreeSet<>(),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate8() {
        assertEquals(parse("2023-01-01"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-01-01"), ONE),
                new TreeSet<>(Set.of(
                        parse("2023-01-01")
                )),
                BEGINNING_OF_DAY
        ));
        assertEquals(parse("2023-01-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-01-01"), ONE),
                new TreeSet<>(Set.of(
                        parse("2023-01-01")
                )),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate9() {
        assertEquals(parse("2023-01-01"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-01-10"), ONE),
                new TreeSet<>(Set.of(
                        parse("2023-01-01")
                )),
                BEGINNING_OF_DAY
        ));
        assertEquals(parse("2023-01-01"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-01-10"), ONE),
                new TreeSet<>(Set.of(
                        parse("2023-01-01")
                )),
                END_OF_DAY
        ));
        assertEquals(parse("2023-01-01"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-01-02"), ONE,
                        parse("2023-01-10"), ONE
                ),
                new TreeSet<>(Set.of(
                        parse("2023-01-01")
                )),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate10() {
        assertEquals(parse("2023-01-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-01-01"), ONE,
                        parse("2023-01-10"), ONE
                ),
                new TreeSet<>(Set.of(
                        parse("2023-01-10")
                )),
                BEGINNING_OF_DAY
        ));
        assertEquals(parse("2023-01-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-01-01"), ONE,
                        parse("2023-01-10"), ONE
                ),
                new TreeSet<>(Set.of(
                        parse("2023-01-10")
                )),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate11() {
        assertEquals(parse("2023-01-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-01-01"), ONE,
                        parse("2023-01-10"), ONE
                ),
                new TreeSet<>(Set.of(
                        parse("2023-01-10"),
                        parse("2023-01-11")
                )),
                BEGINNING_OF_DAY
        ));
        assertEquals(parse("2023-01-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-01-01"), ONE,
                        parse("2023-01-10"), ONE
                ),
                new TreeSet<>(Set.of(
                        parse("2023-01-10"),
                        parse("2023-01-11")
                )),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentStartDate12() {
        assertEquals(parse("2023-02-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-01"), ONE,
                        parse("2023-02-10"), ONE
                ),
                new TreeSet<>(Set.of(
                        parse("2023-02-10"),
                        parse("2023-02-11")
                )),
                BEGINNING_OF_DAY
        ));
        assertEquals(parse("2023-02-02"), PerformanceAnalyzerImpl.detectInvestmentStartDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-01"), ONE,
                        parse("2023-02-10"), ONE
                ),
                new TreeSet<>(Set.of(
                        parse("2023-02-10"),
                        parse("2023-02-11")
                )),
                END_OF_DAY
        ));
    }

    @Test
    void detectInvestmentEndDate1() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(),
                new TreeSet<>()
        ));
    }

    @Test
    void detectInvestmentEndDate2() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(parse("2023-02-28"), ZERO),
                new TreeSet<>()
        ));
    }

    @Test
    void detectInvestmentEndDate3() {
        assertEquals(parse("2023-02-25"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>()
        ));
    }

    @Test
    void detectInvestmentEndDate4() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>()
        ));
    }

    @Test
    void detectInvestmentEndDate5() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>()
        ));
    }

    @Test
    void detectInvestmentEndDate6() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-27"), ONE,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>()
        ));
    }

    @Test
    void detectInvestmentEndDate7() {
        assertEquals(parse("2023-02-27"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-27"), ZERO,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>()
        ));
    }

    @Test
    void detectInvestmentEndDate8() {
        assertEquals(parse("2023-02-27"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-27"), ZERO,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>(Set.of(
                    parse("2023-02-27")
                ))
        ));
    }

    @Test
    void detectInvestmentEndDate9() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-02-28"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-27"), ZERO,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>(Set.of(
                    parse("2023-02-28")
                ))
        ));
    }

    @Test
    void detectInvestmentEndDate10() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-03-31"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-27"), ZERO,
                        parse("2023-02-28"), ZERO
                ),
                new TreeSet<>(Set.of(
                    parse("2023-02-28")
                ))
        ));
    }

    @Test
    void detectInvestmentEndDate11() {
        assertEquals(parse("2023-02-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-03-31"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-27"), ZERO,
                        parse("2023-02-28"), ZERO,
                        parse("2023-03-31"), ZERO
                ),
                new TreeSet<>(Set.of(
                    parse("2023-02-28")
                ))
        ));
    }

    @Test
    void detectInvestmentEndDate12() {
        assertEquals(parse("2023-03-28"), PerformanceAnalyzerImpl.detectInvestmentEndDate(
                parse("2023-01-01"),
                parse("2023-03-31"),
                Map.of(
                        parse("2023-02-25"), ZERO,
                        parse("2023-02-26"), ONE,
                        parse("2023-02-27"), ZERO,
                        parse("2023-03-28"), ZERO,
                        parse("2023-03-31"), ZERO
                ),
                new TreeSet<>(Set.of(
                    parse("2023-02-28")
                ))
        ));
    }

}
