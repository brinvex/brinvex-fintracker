package com.brinvex.ptfactivity.core.api;

import com.brinvex.dms.api.Dms;

import java.util.function.Supplier;

public interface ModuleContext {

    <SERVICE> SERVICE singletonService(Class<SERVICE> type, Supplier<SERVICE> serviceSupplier);

    default String getProperty(String propKey) {
        return getProperty(propKey, null);
    }

    String getProperty(String propKey, String defaultValue);

    Dms dms();

    Toolbox toolbox();

}
