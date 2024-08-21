package com.brinvex.fintracker.api.domain;

import java.io.Serializable;

public record Asset(
        String id,
        AssetType type,
        String country,
        String symbol,
        String name,
        String countryFigi,
        String isin
) implements Serializable {
}
