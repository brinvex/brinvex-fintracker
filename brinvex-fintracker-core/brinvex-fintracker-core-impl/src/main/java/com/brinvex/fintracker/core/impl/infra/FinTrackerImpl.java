package com.brinvex.fintracker.core.impl.infra;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.FinTrackerConfig;
import com.brinvex.fintracker.core.api.FinTrackerModule;
import com.brinvex.fintracker.core.api.facade.HttpClientFacade;
import com.brinvex.fintracker.core.api.facade.ValidatorFacade;
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
    public <MODULE extends FinTrackerModule> MODULE get(Class<MODULE> moduleType) {
        FinTrackerModule module = modules.get(moduleType);
        if (module == null) {
            synchronized (modules) {
                module = modules.get(moduleType);
                if (module == null) {
                    List<MODULE> instances = ServiceLoader.load(moduleType).stream().map(ServiceLoader.Provider::get).toList();
                    module = switch (instances.size()) {
                        case 0:
                            throw new IllegalStateException("Module implementation not found: %s".formatted(moduleType));
                        case 1:
                            FinTrackerModule instance = instances.getFirst();
                            setupApplicationAwareModule(instance);
                            yield instance;
                        default:
                            throw new IllegalStateException("Multiple implementations found: %s, %s".formatted(moduleType, instances));
                    };
                    Assert.notNull(module);
                    modules.put(moduleType, module);
                }
            }
        }
        @SuppressWarnings("unchecked")
        MODULE typedModule = (MODULE) module;
        return typedModule;
    }

    private void setupApplicationAwareModule(FinTrackerModule awareCandidate) {
        if (awareCandidate instanceof FinTrackerModule.ApplicationAware applicationAwareModule) {
            applicationAwareModule.setFinTracker(this);
        }
    }
}
