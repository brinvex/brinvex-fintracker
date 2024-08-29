package com.brinvex.fintracker.api.model.builder;

import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.AssetType;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true, chain = true)
public class AssetBuilder {
    private String id;
    private AssetType type;
    private String country;
    private String symbol;
    private String name;
    private String countryFigi;
    private String isin;
    private String extraType;
    private String extraDetail;

    public Asset build() {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol cannot be null or blank");
        }
        return new Asset(
                id,
                type,
                country,
                symbol,
                name,
                countryFigi,
                isin,
                extraType,
                extraDetail
        );
    }
}
