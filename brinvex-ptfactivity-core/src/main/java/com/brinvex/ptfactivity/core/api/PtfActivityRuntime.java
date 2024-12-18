package com.brinvex.ptfactivity.core.api;

import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.internal.PtfActivityRuntimeImpl;

public interface PtfActivityRuntime {

    PtfActivity process(PtfActivityReq request);

    <MODULE extends Module> MODULE getModule(Class<MODULE> moduleType);

    static PtfActivityRuntime newPtfActivityRuntime(PtfActivityRuntimeConfig config) {
        return new PtfActivityRuntimeImpl(config);
    }


}
