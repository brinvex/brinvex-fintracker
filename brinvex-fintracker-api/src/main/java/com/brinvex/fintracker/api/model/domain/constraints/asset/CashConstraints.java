package com.brinvex.fintracker.api.model.domain.constraints.asset;

import com.brinvex.fintracker.api.model.domain.Asset;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

public class CashConstraints extends AssetConstraints {

    CashConstraints(Asset asset) {
        super(asset);
    }

    @Null
    @Override
    public String getCountry() {
        return super.getCountry();
    }

    @NotNull
    @Override
    public String getSymbol() {
        return super.getSymbol();
    }

    @Null
    @Override
    public String getName() {
        return super.getName();
    }

    @Null
    @Override
    public String getCountryFigi() {
        return super.getCountryFigi();
    }

    @Null
    @Override
    public String getIsin() {
        return super.getIsin();
    }

    @Null
    @Override
    public String getExtraType() {
        return super.getExtraType();
    }

    @Null
    @Override
    public String getExtraDetail() {
        return super.getExtraDetail();
    }
}
