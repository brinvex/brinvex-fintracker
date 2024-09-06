package com.brinvex.fintracker.core.api.provider;

import com.brinvex.fintracker.core.api.model.domain.PtfProgress;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

public interface PtfProgressProvider extends Provider<PtfProgressProvider.PtfProgressReq, PtfProgress> {

    record PtfProgressReq(
            String institution,
            Map<String, String> ptfProps,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance
    ) {
    }

}
