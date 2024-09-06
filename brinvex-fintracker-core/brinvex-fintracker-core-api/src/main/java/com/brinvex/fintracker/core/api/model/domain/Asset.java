package com.brinvex.fintracker.core.api.model.domain;

import lombok.Builder;
import lombok.experimental.Accessors;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

@Builder
@Accessors(fluent = true, chain = true)
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

    public Asset {
        requireNonNull(type);
    }
}
