package com.dreammapper.service;

import com.dreammapper.dto.AnalysisRequestDTO;
import com.dreammapper.dto.AnalysisResultDTO;

public interface AnalysisService {
	AnalysisResultDTO analyzeDream(AnalysisRequestDTO request);

}
