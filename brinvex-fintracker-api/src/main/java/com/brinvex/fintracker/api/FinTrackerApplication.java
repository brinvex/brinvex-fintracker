package com.brinvex.fintracker.api;

import com.brinvex.fintracker.api.facade.HttpClientFacade;
import com.brinvex.util.dms.api.DmsFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface FinTrackerApplication {

    <MODULE extends FinTrackerModule> MODULE get(Class<MODULE> factoryType);

    <BEAN> BEAN get(Class<BEAN> type, Supplier<BEAN> supplier);

    default String property(String propKey) {
        return property(propKey, null);
    }

    String property(String propKey, String defaultValue);

    DmsFactory dmsFactory();

    HttpClientFacade httpClientFacade();

    class Internal {
        private static final Map<FinTrackerConfig, FinTrackerApplication> instances = new ConcurrentHashMap<>();

        private static FinTrackerApplication createNew(FinTrackerConfig config) {
            try {
                return (FinTrackerApplication) Class.forName("com.brinvex.fintracker.core.impl.infra.FinTrackerApplicationImpl")
                        .getConstructor(FinTrackerConfig.class)
                        .newInstance(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static FinTrackerApplication get(FinTrackerConfig config) {
        return Internal.instances.computeIfAbsent(config, _ -> Internal.createNew(config));
    }


}
