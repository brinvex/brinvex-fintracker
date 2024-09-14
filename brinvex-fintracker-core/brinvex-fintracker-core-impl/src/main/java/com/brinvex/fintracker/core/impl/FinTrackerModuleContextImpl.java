package com.brinvex.fintracker.core.impl;

import com.brinvex.fintracker.core.api.facade.HttpClientFacade;
import com.brinvex.fintracker.core.api.facade.ValidatorFacade;
import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.fintracker.core.api.internal.FinTrackerModuleContext;
import com.brinvex.util.dms.api.Dms;
import com.brinvex.util.java.validation.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class FinTrackerModuleContextImpl implements FinTrackerModuleContext {

    private static final Logger LOG = LoggerFactory.getLogger(FinTrackerModuleContextImpl.class);

    private final FinTrackerSharedContext sharedContext;

    private final Class<? extends FinTrackerModule> moduleType;

    private final String moduleName;

    private volatile Dms dms;

    private final Map<Class<?>, Object> singletons = new LinkedHashMap<>();

    public FinTrackerModuleContextImpl(
            FinTrackerSharedContext sharedContext,
            Class<? extends FinTrackerModule> moduleType
    ) {
        this.sharedContext = sharedContext;
        this.moduleType = moduleType;
        this.moduleName = moduleType.getSimpleName();
    }

    @Override
    public <SERVICE> SERVICE singletonService(Class<SERVICE> type, Supplier<SERVICE> supplier) {
        Object uncheckedSingleton = singletons.get(type);
        if (uncheckedSingleton == null) {
            synchronized (singletons) {
                uncheckedSingleton = singletons.get(type);
                if (uncheckedSingleton == null) {
                    uncheckedSingleton = supplier.get();
                    Assert.notNull(uncheckedSingleton);
                    singletons.put(type, uncheckedSingleton);
                }
            }
        }
        @SuppressWarnings("unchecked")
        SERVICE typedSingleton = (SERVICE) uncheckedSingleton;
        return typedSingleton;
    }

    @Override
    public String getProperty(String propKey, String defaultValue) {
        return sharedContext.config().getModuleProperty(moduleType, propKey, defaultValue);
    }

    @Override
    public Map<String, String> getSubProperties(String propPrefix) {
        return sharedContext.config().getModuleSubProperties(moduleType, propPrefix);
    }

    @Override
    public Dms dms() {
        if (dms == null) {
            synchronized (this) {
                if (dms == null) {
                    String dmsWorkspace = getProperty(FinTrackerModule.PropKey.dmsWorkspace, "dms");
                    dms = sharedContext.dmsFactory().getDms(moduleName + "/" + dmsWorkspace);
                }
            }
        }
        return dms;
    }

    @Override
    public HttpClientFacade httpClientFacade() {
        return sharedContext.httpClientFacade();
    }

    @Override
    public ValidatorFacade validator() {
        return sharedContext.validator();
    }
}
