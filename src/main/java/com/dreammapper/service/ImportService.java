package com.dreammapper.service;

import java.util.List;

import com.dreammapper.dto.DreamDTO;
import com.dreammapper.model.User;

public interface ImportService {
	
	/**
	 * JSON formatındaki rüyaları import eder
	 * @param jsonData JSON string
	 * @param user Kullanıcı (import edilen rüyalar bu kullanıcıya ait olacak)
	 * @return Import edilen rüya sayısı ve hata mesajları
	 */
	ImportResult importDreamsFromJson(String jsonData, User user);
	
	/**
	 * Import sonuçlarını tutan inner class
	 */
	record ImportResult(int successCount, int errorCount, List<String> errors) {}
}

