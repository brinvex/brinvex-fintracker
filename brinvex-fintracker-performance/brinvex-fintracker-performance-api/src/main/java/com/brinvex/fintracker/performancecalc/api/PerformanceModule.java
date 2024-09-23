package com.brinvex.fintracker.performancecalc.api;

import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.LinkedModifiedDietzTwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.ModifiedDietzMwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.SimpleReturnCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TrueTwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TwrCalculator;

public interface PerformanceModule extends FinTrackerModule {

    TrueTwrCalculator trueTwrCalculator();

    LinkedModifiedDietzTwrCalculator linkedModifiedDietzTwrCalculator();

    ModifiedDietzMwrCalculator modifiedDietzMwrCalculator();

    SimpleReturnCalculator simpleReturnCalculator();

    /**
     * Returns default TWR Calculator
     */
    TwrCalculator twrCalculator();

    /**
     * Returns default MWR Calculator
     */
    MwrCalculator mwrCalculator();

    <CALCULATOR extends TwrCalculator> CALCULATOR twrCalculator(Class<CALCULATOR> twrCalculatorType);

    <CALCULATOR extends MwrCalculator> CALCULATOR mwrCalculator(Class<CALCULATOR> mwrCalculatorType);

    PerformanceAnalyzer performanceAnalyzer();
}
