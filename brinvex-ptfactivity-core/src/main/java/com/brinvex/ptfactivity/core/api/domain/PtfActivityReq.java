package com.brinvex.ptfactivity.core.api.domain;

import java.time.Duration;
import java.time.LocalDate;

public record PtfActivityReq(
        String providerName,
        Account account,
        LocalDate fromDateIncl,
        LocalDate toDateIncl,
        Duration staleTolerance
) {
}
