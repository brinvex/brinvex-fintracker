package com.brinvex.fintracker.core.api.internal;

public interface FinTrackerModuleFactory<MODULE extends FinTrackerModule> {

    Class<MODULE> moduleType();

    MODULE createModule(FinTrackerModuleContext finTrackerCtx);

}
