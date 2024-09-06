package com.brinvex.fintracker.core.api;

import com.brinvex.util.dms.api.DmsFactory;
import jakarta.validation.ValidatorFactory;

import java.util.function.Supplier;

public interface FinTrackerConfigBuilder {

    FinTrackerConfig build();

    FinTrackerConfigBuilder property(String key, String value);

    FinTrackerConfigBuilder dmsFactory(Supplier<DmsFactory> dmsFactory);

    FinTrackerConfigBuilder validatorFactory(Supplier<ValidatorFactory> validatorFactory);

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
