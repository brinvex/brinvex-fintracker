package com.brinvex.fintracker.performancecalc.api;

import com.brinvex.fintracker.core.api.FinTrackerModule;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;

public interface PerformanceModule extends FinTrackerModule {

    PerformanceCalculator performanceCalculator();
}
