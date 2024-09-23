package com.brinvex.fintracker.performancecalc.impl;

import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleContext;
import com.brinvex.fintracker.performancecalc.api.PerformanceModule;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceAnalyzer;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.MwrCalculator;
import com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TwrCalculator;
import com.brinvex.fintracker.performancecalc.impl.service.LinkedModifiedDietzTwrCalculatorImpl;
import com.brinvex.fintracker.performancecalc.impl.service.ModifiedDietzMwrCalculatorImpl;
import com.brinvex.fintracker.performancecalc.impl.service.PerformanceAnalyzerImpl;
import com.brinvex.fintracker.performancecalc.impl.service.SimpleReturnCalculatorImpl;
import com.brinvex.fintracker.performancecalc.impl.service.TrueTwrCalculatorImpl;

import static com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.LinkedModifiedDietzTwrCalculator;
import static com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.ModifiedDietzMwrCalculator;
import static com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.SimpleReturnCalculator;
import static com.brinvex.fintracker.performancecalc.api.service.PerformanceCalculator.TrueTwrCalculator;

public class PerformanceModuleImpl implements PerformanceModule, FinTrackerModule {

    private final FinTrackerModuleContext finTrackerCtx;

    public PerformanceModuleImpl(FinTrackerModuleContext finTrackerCtx) {
        this.finTrackerCtx = finTrackerCtx;
    }

    @Override
    public TrueTwrCalculator trueTwrCalculator() {
        return finTrackerCtx.singletonService(TrueTwrCalculator.class, TrueTwrCalculatorImpl::new);
    }

    @Override
    public LinkedModifiedDietzTwrCalculator linkedModifiedDietzTwrCalculator() {
        return finTrackerCtx.singletonService(LinkedModifiedDietzTwrCalculator.class,
                () -> new LinkedModifiedDietzTwrCalculatorImpl(modifiedDietzMwrCalculator()));
    }

    @Override
    public ModifiedDietzMwrCalculator modifiedDietzMwrCalculator() {
        return finTrackerCtx.singletonService(ModifiedDietzMwrCalculator.class, ModifiedDietzMwrCalculatorImpl::new);
    }

    @Override
    public SimpleReturnCalculator simpleReturnCalculator() {
        return finTrackerCtx.singletonService(SimpleReturnCalculator.class, SimpleReturnCalculatorImpl::new);
    }

    @Override
    public TwrCalculator twrCalculator() {
        return linkedModifiedDietzTwrCalculator();
    }

    @Override
    public MwrCalculator mwrCalculator() {
        return modifiedDietzMwrCalculator();
    }

    @Override
    public <CALCULATOR extends TwrCalculator> CALCULATOR twrCalculator(Class<CALCULATOR> twrCalculatorType) {
        TwrCalculator uncheckedCalculatorInstance;
        if (twrCalculatorType == LinkedModifiedDietzTwrCalculator.class) {
            uncheckedCalculatorInstance = linkedModifiedDietzTwrCalculator();
        } else if (twrCalculatorType == TrueTwrCalculator.class) {
            uncheckedCalculatorInstance = trueTwrCalculator();
        } else if (twrCalculatorType == TwrCalculator.class) {
            uncheckedCalculatorInstance = twrCalculator();
        } else {
            uncheckedCalculatorInstance = finTrackerCtx.singletonService(twrCalculatorType, () -> {
                throw new IllegalArgumentException("No TwrCalculator registered for type: %s ".formatted(twrCalculatorType));
            });
        }
        @SuppressWarnings("unchecked")
        CALCULATOR typedCalculatorInstance = (CALCULATOR) uncheckedCalculatorInstance;
        return typedCalculatorInstance;
    }

    @Override
    public <CALCULATOR extends MwrCalculator> CALCULATOR mwrCalculator(Class<CALCULATOR> mwrCalculatorType) {
        MwrCalculator uncheckedCalculatorInstance;
        if (mwrCalculatorType == ModifiedDietzMwrCalculator.class) {
            uncheckedCalculatorInstance = modifiedDietzMwrCalculator();
        } else if (mwrCalculatorType == MwrCalculator.class) {
            uncheckedCalculatorInstance = mwrCalculator();
        } else {
            uncheckedCalculatorInstance = finTrackerCtx.singletonService(mwrCalculatorType, () -> {
                throw new IllegalArgumentException("No MwrCalculator registered for type: %s ".formatted(mwrCalculatorType));
            });
        }
        @SuppressWarnings("unchecked")
        CALCULATOR typedCalculatorInstance = (CALCULATOR) uncheckedCalculatorInstance;
        return typedCalculatorInstance;
    }

    @Override
    public PerformanceAnalyzer performanceAnalyzer() {
        return finTrackerCtx.singletonService(PerformanceAnalyzer.class, () ->
                new PerformanceAnalyzerImpl(this::twrCalculator, this::mwrCalculator));
    }
}
