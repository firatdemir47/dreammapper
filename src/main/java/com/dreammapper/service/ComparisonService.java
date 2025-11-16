package com.dreammapper.service;

import com.dreammapper.dto.DreamComparisonDTO;
import com.dreammapper.model.Dream;
import com.dreammapper.model.User;

public interface ComparisonService {
	
	/**
	 * İki rüyayı karşılaştırır
	 * @param dream1 İlk rüya
	 * @param dream2 İkinci rüya
	 * @param user Kullanıcı (sadece kendi rüyalarını karşılaştırabilir)
	 * @return Karşılaştırma sonuçları
	 */
	DreamComparisonDTO compareDreams(Dream dream1, Dream dream2, User user);
	
	/**
	 * İki rüya ID'si ile karşılaştırma yapar
	 * @param dream1Id İlk rüya ID
	 * @param dream2Id İkinci rüya ID
	 * @param user Kullanıcı
	 * @return Karşılaştırma sonuçları
	 */
	DreamComparisonDTO compareDreamsById(Long dream1Id, Long dream2Id, User user);
}

