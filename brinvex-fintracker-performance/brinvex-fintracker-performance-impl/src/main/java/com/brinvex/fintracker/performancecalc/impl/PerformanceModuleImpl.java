package com.brinvex.fintracker.performancecalc.impl;

import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleContext;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceAnalyzerImpl;
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

    @Override
    public PerformanceAnalyzer performanceAnalyzer() {
        return finTrackerCtx.singletonService(PerformanceAnalyzer.class, () -> new PerformanceAnalyzerImpl(performanceCalculator()));
    }
}
