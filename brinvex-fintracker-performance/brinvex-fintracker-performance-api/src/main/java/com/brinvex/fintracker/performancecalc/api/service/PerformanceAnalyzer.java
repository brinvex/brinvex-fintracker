package com.brinvex.fintracker.performancecalc.api.service;

import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysisRequest;
import com.brinvex.fintracker.performancecalc.api.model.PerfAnalysis;

import java.util.List;

public interface PerformanceAnalyzer {

    List<PerfAnalysis> analyzePerformance(PerfAnalysisRequest perfAnalysisRequest);

}
