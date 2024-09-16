package com.brinvex.fintracker.performancecalc.impl;

import com.brinvex.fintracker.core.api.internal.FinTrackerModuleContext;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleFactory;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;

public class PerformanceModuleFactory implements FinTrackerModuleFactory<PerformanceModule> {

    @Override
    public Class<PerformanceModule> moduleType() {
        return PerformanceModule.class;
    }

    @Override
    public PerformanceModule createModule(FinTrackerModuleContext finTrackerCtx) {
        return new PerformanceModuleImpl(finTrackerCtx);
    }
}
