package com.brinvex.fintracker.connector.ibkr.impl;

import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleContext;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleFactory;

public class IbkrModuleFactory implements FinTrackerModuleFactory<IbkrModule> {

    @Override
    public Class<IbkrModule> moduleType() {
        return IbkrModule.class;
    }

    @Override
    public IbkrModule createModule(FinTrackerModuleContext finTrackerCtx) {
        return new IbkrModuleImpl(finTrackerCtx);
    }
}
