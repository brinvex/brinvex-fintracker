package com.brinvex.fintracker.core.impl;

import com.brinvex.fintracker.core.api.FinTrackerConfig;
import com.brinvex.fintracker.core.api.facade.HttpClientFacade;
import com.brinvex.fintracker.core.api.facade.ValidatorFacade;
import com.brinvex.fintracker.core.impl.facade.HttpClientFacadeImpl;
import com.brinvex.fintracker.core.impl.facade.ValidatorFacadeImpl;
import com.brinvex.util.dms.api.DmsFactory;
import com.brinvex.util.java.validation.Validate;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class FinTrackerSharedContext {

    private static final Logger LOG = LoggerFactory.getLogger(FinTrackerSharedContext.class);

    private final FinTrackerConfig config;

    private volatile DmsFactory dmsFactory;

    private volatile HttpClientFacade httpClientFacade;

    private volatile ValidatorFacade validator;

    public FinTrackerSharedContext(FinTrackerConfig config) {
        Validate.notNull(config);
        this.config = config;
    }

    public FinTrackerConfig config() {
        return config;
    }

    public DmsFactory dmsFactory() {
        if (dmsFactory == null) {
            synchronized (this) {
                if (dmsFactory == null) {
                    Supplier<DmsFactory> dmsFactorySupplier = config.dmsFactory();
                    if (dmsFactorySupplier == null) {
                        throw new IllegalStateException("dmsFactory supplier is null");
                    }
                    dmsFactory = dmsFactorySupplier.get();
                }
            }
        }
        return dmsFactory;
    }

    public HttpClientFacade httpClientFacade() {
        if (httpClientFacade == null) {
            synchronized (this) {
                if (httpClientFacade == null) {
                    httpClientFacade = new HttpClientFacadeImpl();
                }
            }
        }
        return httpClientFacade;
    }

    public ValidatorFacade validator() {
        if (validator == null) {
            synchronized (this) {
                if (validator == null) {
                    Supplier<ValidatorFactory> validatorFactorySupplier = config.validatorFactory();
                    jakarta.validation.Validator jakartaValidator;
                    if (validatorFactorySupplier != null) {
                        jakartaValidator = validatorFactorySupplier.get().getValidator();
                    } else {
                        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
                            jakartaValidator = validatorFactory.getValidator();
                        }
                    }
                    validator = new ValidatorFacadeImpl(jakartaValidator);
                }
            }
        }
        return validator;
    }
}
