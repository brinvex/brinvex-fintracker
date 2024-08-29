package com.brinvex.fintracker.api;

import com.brinvex.util.dms.api.DmsFactory;

import java.util.function.Supplier;

public interface FinTrackerConfigBuilder {

    FinTrackerConfig build();

    FinTrackerConfigBuilder dmsFactory(Supplier<DmsFactory> dmsFactory);

    FinTrackerConfigBuilder property(String key, String value);

    static FinTrackerConfigBuilder createDefault() {
        try {
            return (FinTrackerConfigBuilder) Class.forName("com.brinvex.fintracker.core.impl.infra.FinTrackerConfigBuilderImpl")
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
