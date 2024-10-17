package com.brinvex.fintracker.performancecalc.api.service;

import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;

import java.util.SequencedCollection;

public interface PerformanceAnalyzer {

    SequencedCollection<PerfAnalysis> analyzePerformance(PerfAnalysisRequest perfAnalysisRequest);

}
