package com.brinvex.fintracker.api.model.domain.constraints.asset;

import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.AssetType;
import com.brinvex.fintracker.api.model.general.Validatable;
import jakarta.validation.constraints.NotNull;

public abstract class AssetConstraints implements Validatable {

    public static AssetConstraints of(Asset asset) {
        AssetType type = asset.type();
        return switch (type.category()) {
            case CASH -> new CashConstraints(asset);
            case INSTRUMENT -> new InstrumentConstraints(asset);
        };
    }

    private final Asset asset;

    AssetConstraints(Asset asset) {
        this.asset = asset;
    }

    public String getId() {
        return asset.id();
    }

    @NotNull
    public AssetType getType() {
        return asset.type();
    }

    public String getCountry() {
        return asset.country();
    }

    public String getSymbol() {
        return asset.symbol();
    }

    public String getName() {
        return asset.name();
    }

    public String getCountryFigi() {
        return asset.countryFigi();
    }

    public String getIsin() {
        return asset.isin();
    }

    public String getExtraType() {
        return asset.extraType();
    }

    public String getExtraDetail() {
        return asset.extraDetail();
    }
}
