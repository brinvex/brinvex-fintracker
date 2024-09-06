package com.brinvex.fintracker.core.api;

import com.brinvex.util.dms.api.DmsFactory;
import jakarta.validation.ValidatorFactory;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public record FinTrackerConfig(
        Map<String, String> properties,
        Supplier<DmsFactory> dmsFactory,
        Supplier<ValidatorFactory> validatorFactory
) {

    public FinTrackerConfig(
            Map<String, String> properties,
            Supplier<DmsFactory> dmsFactory,
            Supplier<ValidatorFactory> validatorFactory
    ) {
        this.dmsFactory = dmsFactory;
        this.validatorFactory = validatorFactory;
        this.properties = properties == null ? emptyMap() : Map.copyOf(properties);
    }

    public static FinTrackerConfigBuilder builder() {
        return FinTrackerConfigBuilder.createDefault();
    }

}
