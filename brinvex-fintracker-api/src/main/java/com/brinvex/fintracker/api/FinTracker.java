package com.brinvex.fintracker.api;

import com.brinvex.fintracker.api.facade.HttpClientFacade;
import com.brinvex.fintracker.api.facade.ValidatorFacade;
import com.brinvex.util.dms.api.DmsFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface FinTracker {

    <MODULE extends FinTrackerModule> MODULE get(Class<MODULE> factoryType);

    <BEAN> BEAN get(Class<BEAN> type, Supplier<BEAN> supplier);

    default String property(String propKey) {
        return property(propKey, null);
    }

    String property(String propKey, String defaultValue);

    DmsFactory dmsFactory();

    HttpClientFacade httpClientFacade();

    ValidatorFacade validator();

    class Internal {
        private static final Map<FinTrackerConfig, FinTracker> instances = new ConcurrentHashMap<>();

        private static FinTracker createNew(FinTrackerConfig config) {
            try {
                return (FinTracker) Class.forName("com.brinvex.fintracker.core.impl.infra.FinTrackerImpl")
                        .getConstructor(FinTrackerConfig.class)
                        .newInstance(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static FinTracker get(FinTrackerConfig config) {
        return Internal.instances.computeIfAbsent(config, _ -> Internal.createNew(config));
    }


}
