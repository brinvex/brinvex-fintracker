package com.brinvex.fintracker.api;

import com.brinvex.util.dms.api.DmsFactory;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public record FinTrackerConfig(
        Map<String, String> properties,
        Supplier<DmsFactory> dmsFactory
) {

    public FinTrackerConfig(Map<String, String> properties, Supplier<DmsFactory> dmsFactory) {
        this.dmsFactory = dmsFactory;
        this.properties = properties == null ? emptyMap() : Map.copyOf(properties);
    }

    public static FinTrackerConfigBuilder builder() {
        return FinTrackerConfigBuilder.createDefault();
    }

}
