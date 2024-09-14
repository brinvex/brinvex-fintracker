package com.brinvex.fintracker.performancecalc.impl.infra;

import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleContext;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceCalculatorImpl;

public class PerformanceModuleImpl implements PerformanceModule, FinTrackerModule {

    private final FinTrackerModuleContext finTrackerCtx;

    public PerformanceModuleImpl(FinTrackerModuleContext finTrackerCtx) {
        this.finTrackerCtx = finTrackerCtx;
    }

    @Override
    public PerformanceCalculator performanceCalculator() {
        return finTrackerCtx.singletonService(PerformanceCalculator.class, PerformanceCalculatorImpl::new);
    }
}
