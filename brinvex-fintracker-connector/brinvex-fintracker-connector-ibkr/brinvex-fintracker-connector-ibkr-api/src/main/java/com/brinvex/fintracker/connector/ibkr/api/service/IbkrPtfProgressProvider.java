package com.brinvex.fintracker.connector.ibkr.api.service;

import com.brinvex.fintracker.api.model.domain.PtfProgress;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrCredentials;

import java.time.Duration;
import java.time.LocalDate;

public interface IbkrPtfProgressProvider {
    PtfProgress getPortfolioProgressOffline(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl);

    PtfProgress getPortfolioProgressOnline(
            String accountId,
            IbkrCredentials credentials,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance
    );
}
