package com.brinvex.ptfactivity.connector.rvlt.api.service;

import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.provider.PtfActivityProvider;

import java.time.LocalDate;

public interface RvltPtfActivityProvider extends PtfActivityProvider {

    PtfActivity getPtfProgress(
            Account account,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    );
}
