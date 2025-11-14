package com.dreammapper.service;

import com.dreammapper.dto.StatisticsDTO;
import com.dreammapper.model.User;

public interface StatisticsService {
	
	StatisticsDTO getUserStatistics(Long userId);
	
	StatisticsDTO getUserStatistics(User user);
}

