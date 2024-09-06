package com.brinvex.fintracker.core.impl.infra;

import com.brinvex.fintracker.core.api.FinTrackerConfig;
import com.brinvex.fintracker.core.api.FinTrackerConfigBuilder;
import com.brinvex.util.dms.api.DmsFactory;
import jakarta.validation.ValidatorFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FinTrackerConfigBuilderImpl implements FinTrackerConfigBuilder {

    private final Map<String, String> properties = new LinkedHashMap<>();

    private Supplier<DmsFactory> dmsFactory;

    private Supplier<ValidatorFactory> validatorFactory;

    @Override
    public FinTrackerConfig build() {
        return new FinTrackerConfig(properties, dmsFactory, validatorFactory);
    }

    @Override
    public FinTrackerConfigBuilder property(String key, String value) {
        properties.put(key, value);
        return this;
    }

    @Override
    public FinTrackerConfigBuilder dmsFactory(Supplier<DmsFactory> dmsFactory) {
        this.dmsFactory = dmsFactory;
        return this;
    }

    @Override
    public FinTrackerConfigBuilder validatorFactory(Supplier<ValidatorFactory> validatorFactory) {
        this.validatorFactory = validatorFactory;
        return this;
    }
}
