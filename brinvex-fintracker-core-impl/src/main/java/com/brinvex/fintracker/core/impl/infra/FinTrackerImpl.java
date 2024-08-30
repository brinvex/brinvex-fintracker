package com.brinvex.fintracker.core.impl.infra;

import com.brinvex.fintracker.api.FinTracker;
import com.brinvex.fintracker.api.FinTrackerConfig;
import com.brinvex.fintracker.api.FinTrackerModule;
import com.brinvex.fintracker.api.facade.HttpClientFacade;
import com.brinvex.fintracker.api.facade.ValidatorFacade;
import com.brinvex.fintracker.core.impl.facade.HttpClientFacadeImpl;
import com.brinvex.fintracker.core.impl.facade.ValidatorFacadeImpl;
import com.brinvex.util.dms.api.DmsFactory;
import com.brinvex.util.java.validation.Assert;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class FinTrackerImpl implements FinTracker {

    private static final Logger LOG = LoggerFactory.getLogger(FinTrackerImpl.class);

    private final FinTrackerConfig config;

    private volatile DmsFactory dmsFactory;

    private volatile HttpClientFacade httpClientFacade;

    private volatile ValidatorFacade validator;

    private final Map<Class<?>, FinTrackerModule> modules = new LinkedHashMap<>();

    private final Map<Class<?>, Object> singletons = new LinkedHashMap<>();

    public FinTrackerImpl(
            FinTrackerConfig config
    ) {
        LOG.debug("Instantiating FinTrackerApplicationImpl with config: {}", config);
        this.config = config;
    }

    @Override
    public String property(String propKey, String defaultValue) {
        return this.config.properties().getOrDefault(propKey, defaultValue);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public <T> T get(Class<T> type, Supplier<T> supplier) {
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
        T typedSingleton = (T) uncheckedSingleton;
        return typedSingleton;
    }

    @Override
    public <MODULE extends FinTrackerModule> MODULE get(Class<MODULE> factoryType) {
        FinTrackerModule uncheckedModule = modules.get(factoryType);
        if (uncheckedModule == null) {
            synchronized (singletons) {
                uncheckedModule = modules.get(factoryType);
                if (uncheckedModule == null) {
                    List<FinTrackerModule> instances = ServiceLoader.load(FinTrackerModule.class).stream().map(ServiceLoader.Provider::get).toList();
                    uncheckedModule = switch (instances.size()) {
                        case 0:
                            throw new IllegalStateException("Factory implementation not found: %s".formatted(factoryType));
                        case 1:
                            FinTrackerModule instance = instances.getFirst();
                            setupApplicationAwareModule(instance);
                            yield instance;
                        default:
                            throw new IllegalStateException("Multiple factory implementations found: %s, %s".formatted(factoryType, instances));
                    };
                    Assert.notNull(uncheckedModule);
                    modules.put(factoryType, uncheckedModule);
                }
            }
        }
        @SuppressWarnings("unchecked")
        MODULE typedModule = (MODULE) uncheckedModule;
        return typedModule;
    }

    private void setupApplicationAwareModule(FinTrackerModule awareCandidate) {
        if (awareCandidate instanceof FinTrackerModule.ApplicationAware applicationAwareModule) {
            applicationAwareModule.setApplication(this);
        }
    }
}
