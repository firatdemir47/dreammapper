package com.dreammapper.service;

import java.util.List;

import com.dreammapper.dto.AnalysisRequestDTO;
import com.dreammapper.dto.AnalysisResultDTO;

public interface AnalysisService {
	AnalysisResultDTO analyzeDream(AnalysisRequestDTO request);

	List<AnalysisResultDTO> getDreamAnalysisHistory(Long dreamId, Long userId);
}
