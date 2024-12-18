package com.brinvex.ptfactivity.connector.rvlt.internal;

import com.brinvex.ptfactivity.connector.rvlt.api.RvltModule;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltDms;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMapper;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMerger;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltPtfActivityProvider;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltStatementParser;
import com.brinvex.ptfactivity.connector.rvlt.internal.service.RvltDmsImpl;
import com.brinvex.ptfactivity.connector.rvlt.internal.service.RvltFinTransactionMapperImpl;
import com.brinvex.ptfactivity.connector.rvlt.internal.service.RvltFinTransactionMergerImpl;
import com.brinvex.ptfactivity.connector.rvlt.internal.service.RvltPtfActivityProviderImpl;
import com.brinvex.ptfactivity.connector.rvlt.internal.service.parser.RvltStatementParserImpl;
import com.brinvex.ptfactivity.core.api.Module;
import com.brinvex.ptfactivity.core.api.ModuleContext;
import com.brinvex.ptfactivity.core.api.ModuleFactory;
import com.brinvex.ptfactivity.core.api.provider.Provider;
import com.brinvex.ptfactivity.core.api.provider.PtfActivityProvider;

import java.util.List;
import java.util.SequencedCollection;
import java.util.Set;

import static java.util.Collections.emptyList;

public class RvltModuleImpl implements RvltModule, Module {

    public static class RvltModuleFactory implements ModuleFactory<RvltModule> {

        @Override
        public Class<RvltModule> connectorType() {
            return RvltModule.class;
        }

        @Override
        public Set<Class<? extends Provider<?, ?>>> providerTypes() {
            return Set.of(PtfActivityProvider.class);
        }

        @Override
        public RvltModule createConnector(ModuleContext moduleCtx) {
            return new RvltModuleImpl(moduleCtx);
        }
    }

    private final ModuleContext moduleCtx;

    public RvltModuleImpl(ModuleContext moduleCtx) {
        this.moduleCtx = moduleCtx;
    }

    @Override
    public SequencedCollection<Provider<?, ?>> providers(Class<? extends Provider<?, ?>> providerType) {
        if (providerType == PtfActivityProvider.class) {
            return List.of(ptfProgressProvider());
        }
        return emptyList();
    }

    @Override
    public RvltStatementParser statementParser() {
        return moduleCtx.singletonService(RvltStatementParser.class, () -> new RvltStatementParserImpl(moduleCtx.toolbox().pdfReader()));
    }

    @Override
    public RvltDms dms() {
        return moduleCtx.singletonService(RvltDms.class, () -> new RvltDmsImpl(moduleCtx.dms()));
    }

    @Override
    public RvltFinTransactionMerger finTransactionMerger() {
        return moduleCtx.singletonService(RvltFinTransactionMerger.class, RvltFinTransactionMergerImpl::new);
    }

    @Override
    public RvltFinTransactionMapper finTransactionMapper() {
        return moduleCtx.singletonService(RvltFinTransactionMapper.class, RvltFinTransactionMapperImpl::new);
    }

    @Override
    public RvltPtfActivityProvider ptfProgressProvider() {
        return moduleCtx.singletonService(RvltPtfActivityProvider.class, () -> new RvltPtfActivityProviderImpl(
                dms(),
                statementParser(),
                finTransactionMerger(),
                finTransactionMapper()
        ));
    }
}
