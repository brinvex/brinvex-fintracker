package com.brinvex.fintracker.core.api.model.domain.constraints.asset;

import com.brinvex.fintracker.core.api.model.domain.Asset;
import jakarta.validation.constraints.NotNull;

public class InstrumentConstraints extends AssetConstraints {

    InstrumentConstraints(Asset asset) {
        super(asset);
    }

    @NotNull
    @Override
    public String getCountry() {
        return super.getCountry();
    }

    @NotNull
    @Override
    public String getSymbol() {
        return super.getSymbol();
    }
}
