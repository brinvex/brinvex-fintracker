package com.brinvex.fintracker.core.api.internal;

import com.brinvex.fintracker.core.api.facade.HttpClientFacade;
import com.brinvex.fintracker.core.api.facade.ValidatorFacade;
import com.brinvex.util.dms.api.Dms;

import java.util.Map;
import java.util.function.Supplier;

public interface FinTrackerModuleContext {

    <SERVICE> SERVICE singletonService(Class<SERVICE> type, Supplier<SERVICE> supplier);

    default String getProperty(String propKey) {
        return getProperty(propKey, null);
    }

    String getProperty(String propKey, String defaultValue);

    Map<String, String> getSubProperties(String propPrefix);

    Dms dms();

    HttpClientFacade httpClientFacade();

    ValidatorFacade validator();

}
