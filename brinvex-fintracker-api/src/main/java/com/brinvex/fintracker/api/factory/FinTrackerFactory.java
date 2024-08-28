package com.brinvex.fintracker.api.factory;

import com.brinvex.fintracker.api.facade.HttpClientFacade;

public interface FinTrackerFactory {

    HttpClientFacade httpClientFacade();

    static FinTrackerFactory create() {
        try {
            return (FinTrackerFactory) Class.forName("com.brinvex.fintracker.common.impl.factory.FinTrackerFactoryImpl")
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
