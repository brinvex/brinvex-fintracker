package com.brinvex.ptfactivity.connector.fiob.api.service;

import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.provider.PtfActivityProvider;

import java.time.Duration;
import java.time.LocalDate;

public interface FiobPtfActivityProvider extends PtfActivityProvider {

    PtfActivity getPtfProgress(
            Account account,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance
    );

    PtfActivity getPtfProgressOffline(
            Account fiobAccount,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    );

}
