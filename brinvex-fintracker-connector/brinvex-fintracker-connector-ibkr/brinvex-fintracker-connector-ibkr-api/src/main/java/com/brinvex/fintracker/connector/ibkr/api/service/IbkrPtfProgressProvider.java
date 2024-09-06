package com.brinvex.fintracker.connector.ibkr.api.service;

import com.brinvex.fintracker.core.api.model.domain.PtfProgress;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrAccount;

import java.time.Duration;
import java.time.LocalDate;

public interface IbkrPtfProgressProvider {

    PtfProgress getPortfolioProgress(
            IbkrAccount ibkrAccount,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance
    );

    PtfProgress getPortfolioProgressOffline(
            IbkrAccount ibkrAccount,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    );
}
