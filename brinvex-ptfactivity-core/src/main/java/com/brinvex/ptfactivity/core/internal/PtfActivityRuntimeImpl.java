package com.brinvex.ptfactivity.core.internal;

import com.brinvex.ptfactivity.core.api.CoreModule;
import com.brinvex.ptfactivity.core.api.PtfActivityRuntime;
import com.brinvex.ptfactivity.core.api.PtfActivityRuntimeConfig;
import com.brinvex.ptfactivity.core.api.Module;
import com.brinvex.ptfactivity.core.api.ModuleFactory;
import com.brinvex.ptfactivity.core.api.Toolbox;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.api.provider.Provider;
import com.brinvex.java.validation.Assert;
import com.brinvex.ptfactivity.core.api.provider.PtfActivityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedCollection;
import java.util.ServiceLoader;

import static com.brinvex.java.collection.CollectionUtil.getFirstThrowIfMore;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("unused")
public class PtfActivityRuntimeImpl implements PtfActivityRuntime {

    private final Map<Class<?>, ModuleFactory<?>> moduleFactories;

    private final Map<Class<?>, Module> modules = new LinkedHashMap<>();

    private final Map<Class<?>, List<Provider<?, ?>>> extensionProviders = new LinkedHashMap<>();

    private final Map<Class<?>, Provider<?, ?>> coreProviders = new LinkedHashMap<>();

    private final PtfActivityRuntimeConfig config;

    private volatile Toolbox toolbox;

    private volatile CoreModule core;

    public PtfActivityRuntimeImpl(PtfActivityRuntimeConfig config) {
        LOG.debug("Instantiating PtfActivityRuntimeImpl: {}", config);
        this.config = config;
        this.moduleFactories = loadModuleFactories();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <MODULE extends Module> MODULE getModule(Class<MODULE> moduleType) {
        Module module = modules.get(moduleType);
        if (module == null) {
            if (moduleType == CoreModule.class) {
                return (MODULE) getCoreModule();
            }
            synchronized (modules) {
                module = modules.get(moduleType);
                if (module == null) {
                    ModuleFactory<?> moduleFactory = moduleFactories.get(moduleType);
                    if (moduleFactory == null) {
                        throw new IllegalStateException("Module factory not found: " + moduleType);
                    }
                    module = moduleFactory.createConnector(new ModuleContextImpl(this, config, getToolboxModule(), moduleType));
                    Assert.notNull(module);
                    modules.put(moduleType, module);
                }
            }
        }
        return (MODULE) module;
    }

    @Override
    public PtfActivity process(PtfActivityReq request) {
        return getProvider(PtfActivityProvider.class, request).process(request);
    }

    @SuppressWarnings("unchecked")
    private <REQUEST, PROVIDER extends Provider<REQUEST, ?>> PROVIDER getProvider(Class<PROVIDER> providerType, REQUEST request) {
        List<Provider<?, ?>> extensionProvidersForType = extensionProviders.get(providerType);
        if (extensionProvidersForType == null) {
            synchronized (extensionProviders) {
                extensionProvidersForType = extensionProviders.get(providerType);
                if (extensionProvidersForType == null) {
                    extensionProvidersForType = new ArrayList<>();
                    for (Entry<Class<?>, ModuleFactory<?>> e : moduleFactories.entrySet()) {
                        Class<Module> moduleType = (Class<Module>) e.getKey();
                        ModuleFactory<?> moduleFactory = e.getValue();
                        if (moduleFactory.providerTypes().contains(providerType)) {
                            Module module = getModule(moduleType);
                            SequencedCollection<Provider<?, ?>> moduleProviders = module.providers(providerType);
                            extensionProvidersForType.addAll(moduleProviders);
                        }
                    }
                    LOG.debug("Assigning extension providers: {} -> {}", providerType, extensionProvidersForType);
                    extensionProviders.put(providerType, extensionProvidersForType);
                }
            }
        }
        PROVIDER resultProvider = null;
        for (Provider<?, ?> provider : extensionProvidersForType) {
            PROVIDER typedProvider = (PROVIDER) provider;
            if (typedProvider.supports(request)) {
                if (resultProvider == null) {
                    resultProvider = typedProvider;
                } else {
                    throw new IllegalStateException("Multiple providers found: %s, %s, %s, %s".formatted(providerType, request, resultProvider, typedProvider));
                }
            }
        }
        if (resultProvider == null) {
            PROVIDER coreProvider = (PROVIDER) coreProviders.get(providerType);
            if (coreProvider == null) {
                synchronized (coreProviders) {
                    coreProvider = (PROVIDER) coreProviders.get(providerType);
                    if (coreProvider == null) {
                        coreProvider = (PROVIDER) getFirstThrowIfMore(getCoreModule().providers(providerType));
                        if (coreProvider != null) {
                            coreProviders.put(providerType, coreProvider);
                            LOG.debug("Assigning core provider: {} -> {}", providerType, coreProvider);
                        }
                    }
                }
            }
            if (coreProvider != null) {
                if (coreProvider.supports(request)) {
                    resultProvider = coreProvider;
                }
            }
        }
        if (resultProvider == null) {
            throw new IllegalStateException("Provider not found: %s, %s".formatted(providerType, request));
        }
        return resultProvider;
    }

    private static Map<Class<?>, ModuleFactory<?>> loadModuleFactories() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ServiceLoader<ModuleFactory<?>> serviceLoader = (ServiceLoader) ServiceLoader.load(ModuleFactory.class);
        Map<Class<?>, ModuleFactory<?>> moduleFactories = serviceLoader
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(toMap(ModuleFactory::connectorType, identity()));
        if (moduleFactories.isEmpty()) {
            LOG.info("""
                    No module factories were loaded using SPI. This may indicate an issue with classloading or concurrent access to the ServiceLoader, such as through parallel streams or similar mechanisms.
                    """);
        }
        return moduleFactories;
    }

    private Toolbox getToolboxModule() {
        if (toolbox == null) {
            synchronized (modules) {
                if (toolbox == null) {
                    toolbox = new ToolboxImpl(config);
                }
            }
        }
        return toolbox;
    }

    private CoreModule getCoreModule() {
        if (core == null) {
            synchronized (modules) {
                if (core == null) {
                    core = new CoreModuleImpl(new ModuleContextImpl(this, config, getToolboxModule(), CoreModule.class));
                }
            }
        }
        return core;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PtfActivityRuntimeImpl.class);

}
