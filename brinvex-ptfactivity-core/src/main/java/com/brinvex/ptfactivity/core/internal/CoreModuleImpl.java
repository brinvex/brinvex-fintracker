package com.brinvex.ptfactivity.core.internal;

import com.brinvex.ptfactivity.core.api.CoreModule;
import com.brinvex.ptfactivity.core.api.ModuleContext;
import com.brinvex.ptfactivity.core.api.Toolbox;
import com.brinvex.ptfactivity.core.api.provider.Provider;
import com.brinvex.ptfactivity.core.api.provider.PtfActivityProvider;
import com.brinvex.ptfactivity.core.internal.ptfprogress.PtfActivityProviderImpl;

import java.util.List;
import java.util.SequencedCollection;

import static java.util.Collections.emptyList;

public class CoreModuleImpl implements CoreModule {

    private final ModuleContext moduleCtx;

    public CoreModuleImpl(ModuleContext moduleCtx) {
        this.moduleCtx = moduleCtx;
    }

    @Override
    public SequencedCollection<Provider<?, ?>> providers(Class<? extends Provider<?, ?>> providerType) {
        if (PtfActivityProvider.class == providerType) {
            return List.of(ptfProgressProvider());
        }
        return emptyList();
    }

    private PtfActivityProvider ptfProgressProvider() {
        return moduleCtx.singletonService(PtfActivityProvider.class, () -> new PtfActivityProviderImpl(moduleCtx.dms()));
    }

    @Override
    public Toolbox toolbox() {
        return moduleCtx.toolbox();
    }
}
