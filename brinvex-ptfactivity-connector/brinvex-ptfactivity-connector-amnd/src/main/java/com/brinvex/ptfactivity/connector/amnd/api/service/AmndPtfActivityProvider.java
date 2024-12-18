package com.brinvex.ptfactivity.connector.amnd.api.service;

import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.provider.PtfActivityProvider;

import java.time.LocalDate;

public interface AmndPtfActivityProvider extends PtfActivityProvider {

    PtfActivity getPtfProgress(
            Account account,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    );

}
