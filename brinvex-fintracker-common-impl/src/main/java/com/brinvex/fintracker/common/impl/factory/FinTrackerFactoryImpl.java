package com.brinvex.fintracker.common.impl.factory;

import com.brinvex.fintracker.api.facade.HttpClientFacade;
import com.brinvex.fintracker.api.factory.FinTrackerFactory;
import com.brinvex.fintracker.common.impl.facade.HttpClientFacadeImpl;
import com.brinvex.util.java.LazyConstant;

public class FinTrackerFactoryImpl implements FinTrackerFactory {

    private final LazyConstant<HttpClientFacade> httpClientFacade = LazyConstant.threadSafe(HttpClientFacadeImpl::new);

    @Override
    public HttpClientFacade httpClientFacade() {
        return httpClientFacade.get();
    }
}
