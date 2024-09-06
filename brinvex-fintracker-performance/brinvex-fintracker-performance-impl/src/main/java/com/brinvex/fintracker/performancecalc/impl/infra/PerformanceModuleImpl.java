package com.brinvex.fintracker.performancecalc.impl.infra;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.FinTrackerModule;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceCalculatorImpl;

public class PerformanceModuleImpl implements PerformanceModule, FinTrackerModule.ApplicationAware {

    private FinTracker finTracker;

    @Override
    public PerformanceCalculator performanceCalculator() {
        return finTracker.get(PerformanceCalculator.class, PerformanceCalculatorImpl::new);
    }

    @Override
    public void setFinTracker(FinTracker finTracker) {
        this.finTracker = finTracker;
    }
}
