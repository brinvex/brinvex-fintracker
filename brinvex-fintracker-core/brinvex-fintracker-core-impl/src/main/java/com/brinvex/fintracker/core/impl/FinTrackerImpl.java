package com.brinvex.fintracker.core.impl;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.FinTrackerConfig;
import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleFactory;
import com.brinvex.util.java.validation.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("unused")
public class FinTrackerImpl implements FinTracker {

    private static final Logger LOG = LoggerFactory.getLogger(FinTrackerImpl.class);

    private volatile Map<Class<?>, FinTrackerModuleFactory<?>> moduleFactories;

    private final Map<Class<?>, FinTrackerModule> modules = new LinkedHashMap<>();

    private final FinTrackerSharedContext finTrackerSharedContext;

    public FinTrackerImpl(FinTrackerConfig config) {
        LOG.debug("Instantiating FinTrackerApplicationImpl with config: {}", config);
        this.finTrackerSharedContext = new FinTrackerSharedContext(config);
    }

    @Override
    public <MODULE extends FinTrackerModule> MODULE get(Class<MODULE> moduleType) {
        FinTrackerModule module = modules.get(moduleType);
        if (module == null) {
            synchronized (modules) {
                module = modules.get(moduleType);
                if (module == null) {
                    FinTrackerModuleFactory<?> moduleFactory = getModuleFactories().get(moduleType);
                    if (moduleFactory == null) {
                        throw new IllegalStateException("Module factory not found for type: " + moduleType);
                    }
                    module = moduleFactory.createModule(new FinTrackerModuleContextImpl(finTrackerSharedContext, moduleType));
                    Assert.notNull(module);
                    modules.put(moduleType, module);
                }
            }
        }
        @SuppressWarnings("unchecked")
        MODULE typedModule = (MODULE) module;
        return typedModule;
    }

    private Map<Class<?>, FinTrackerModuleFactory<?>> getModuleFactories() {
        if (moduleFactories == null) {
            synchronized (modules) {
                if (moduleFactories == null) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    ServiceLoader<FinTrackerModuleFactory<?>> serviceLoader = (ServiceLoader) ServiceLoader.load(FinTrackerModuleFactory.class);
                    this.moduleFactories = serviceLoader
                            .stream()
                            .map(ServiceLoader.Provider::get)
                            .collect(toMap(FinTrackerModuleFactory::moduleType, identity()));
                }
            }
        }
        return moduleFactories;
    }
}
