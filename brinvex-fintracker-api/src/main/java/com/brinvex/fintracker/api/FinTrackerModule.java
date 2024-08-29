package com.brinvex.fintracker.api;

public interface FinTrackerModule {

    interface ApplicationAware {

        void setApplication(FinTrackerApplication application);

    }
}
