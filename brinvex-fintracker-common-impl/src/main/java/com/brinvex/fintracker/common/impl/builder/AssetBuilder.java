package com.brinvex.fintracker.common.impl.builder;

import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.AssetType;
import com.brinvex.util.java.validation.Assert;
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
        Assert.notNull(type);
        Assert.notNull(symbol);

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
