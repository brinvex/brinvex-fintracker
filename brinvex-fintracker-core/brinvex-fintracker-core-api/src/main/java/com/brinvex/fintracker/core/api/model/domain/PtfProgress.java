package com.brinvex.fintracker.core.api.model.domain;

import com.brinvex.fintracker.core.api.model.general.DateAmount;

import java.io.Serializable;
import java.util.List;

public record PtfProgress(
        List<FinTransaction> transactions,
        List<DateAmount> netAssetValues,
        String ccy
) implements Serializable {

    public PtfProgress(List<FinTransaction> transactions, List<DateAmount> netAssetValues, String ccy) {
        this.transactions = transactions == null ? null : List.copyOf(transactions);
        this.netAssetValues = netAssetValues == null ? null : List.copyOf(netAssetValues);
        this.ccy = ccy;
    }
}

