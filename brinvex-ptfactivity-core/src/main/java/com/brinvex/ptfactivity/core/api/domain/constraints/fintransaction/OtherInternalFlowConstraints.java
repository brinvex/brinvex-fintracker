package com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction;

import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import jakarta.validation.constraints.Null;

public class OtherInternalFlowConstraints extends FinTransactionConstraints {

    OtherInternalFlowConstraints(FinTransaction finTransaction) {
        super(finTransaction);
    }

    @Null
    @Override
    public Asset getAsset() {
        return super.getAsset();
    }

}
