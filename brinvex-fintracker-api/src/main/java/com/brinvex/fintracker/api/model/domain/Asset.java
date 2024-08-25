package com.brinvex.fintracker.api.model.domain;

import java.io.Serializable;

public record Asset(
        String id,
        AssetType type,
        String country,
        String symbol,
        String name,
        String countryFigi,
        String isin,
        String extraType,
        String extraDetail
) implements Serializable {
}
