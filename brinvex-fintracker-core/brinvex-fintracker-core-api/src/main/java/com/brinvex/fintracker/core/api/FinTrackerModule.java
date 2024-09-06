package com.brinvex.fintracker.core.api;

public interface FinTrackerModule {

    interface ApplicationAware {

        void setFinTracker(FinTracker finTracker);

    }
}
