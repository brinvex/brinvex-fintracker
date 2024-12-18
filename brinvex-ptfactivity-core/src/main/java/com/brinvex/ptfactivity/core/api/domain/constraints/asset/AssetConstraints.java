package com.brinvex.ptfactivity.core.api.domain.constraints.asset;

import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.enu.AssetType;
import com.brinvex.ptfactivity.core.api.general.Regex;
import com.brinvex.ptfactivity.core.api.general.Validatable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public abstract class AssetConstraints implements Validatable {

    public static AssetConstraints of(Asset asset) {
        return switch (asset.type()) {
            case CASH -> new CashConstraints(asset);
            case STOCK, ETF, FUND, BOND, DERIVATIVE -> new InstrumentConstraints(asset);
        };
    }

    private final Asset asset;

    AssetConstraints(Asset asset) {
        this.asset = asset;
    }

    @NotNull
    public AssetType getType() {
        return asset.type();
    }

    @Pattern(regexp = Regex.COUNTRY_2)
    public String getCountry() {
        return asset.country();
    }

    @NotBlank
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

    public String getExternalType() {
        return asset.externalType();
    }

    public String getExternalDetail() {
        return asset.externalDetail();
    }
}
