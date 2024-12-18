package com.brinvex.ptfactivity.core.api.domain;

import com.brinvex.ptfactivity.core.api.domain.enu.AssetType;

public record Asset(
        AssetType type,
        String country,
        String symbol,
        String name,
        String countryFigi,
        String isin,
        String externalType,
        String externalDetail
) {

    public Asset {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
    }

    public static AssetBuilder builder() {
        return new AssetBuilder();
    }

    public static class AssetBuilder {
        private AssetType type;
        private String country;
        private String symbol;
        private String name;
        private String countryFigi;
        private String isin;
        private String externalType;
        private String externalDetail;

        AssetBuilder() {
        }

        public AssetBuilder type(AssetType type) {
            this.type = type;
            return this;
        }

        public AssetBuilder country(String country) {
            this.country = country;
            return this;
        }

        public AssetBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public AssetBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AssetBuilder countryFigi(String countryFigi) {
            this.countryFigi = countryFigi;
            return this;
        }

        public AssetBuilder isin(String isin) {
            this.isin = isin;
            return this;
        }

        public AssetBuilder extraType(String extraType) {
            this.externalType = extraType;
            return this;
        }

        public AssetBuilder extraDetail(String extraDetail) {
            this.externalDetail = extraDetail;
            return this;
        }

        public Asset build() {
            return new Asset(
                    this.type,
                    this.country,
                    this.symbol,
                    this.name,
                    this.countryFigi,
                    this.isin,
                    this.externalType,
                    this.externalDetail);
        }
    }
}
