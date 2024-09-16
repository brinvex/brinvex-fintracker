package com.brinvex.fintracker.core.api;

import com.brinvex.fintracker.core.api.internal.FinTrackerModule;

public interface FinTracker {

    <MODULE extends FinTrackerModule> MODULE get(Class<MODULE> moduleType);

    static FinTracker newInstance() {
        return newInstance(new FinTrackerConfig());
    }

    static FinTracker newInstance(FinTrackerConfig config) {
        try {
            return (FinTracker) Class.forName("com.brinvex.fintracker.core.impl.FinTrackerImpl")
                    .getConstructor(FinTrackerConfig.class)
                    .newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
