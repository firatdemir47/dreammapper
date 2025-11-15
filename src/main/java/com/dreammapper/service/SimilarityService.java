package com.dreammapper.service;

import java.util.List;

import com.dreammapper.dto.SimilarDreamDTO;
import com.dreammapper.model.Dream;
import com.dreammapper.model.User;

public interface SimilarityService {
	
	/**
	 * Verilen rüyaya benzer rüyaları bulur
	 * @param dream Kaynak rüya
	 * @param user Kullanıcı (sadece kendi rüyalarında arama yapılır)
	 * @param limit Maksimum sonuç sayısı
	 * @param minSimilarity Minimum benzerlik skoru (0.0 - 1.0)
	 * @return Benzer rüyalar listesi
	 */
	List<SimilarDreamDTO> findSimilarDreams(Dream dream, User user, int limit, double minSimilarity);
	
	/**
	 * İki rüya arasındaki benzerlik skorunu hesaplar
	 * @param dream1 İlk rüya
	 * @param dream2 İkinci rüya
	 * @return Benzerlik skoru (0.0 - 1.0)
	 */
	double calculateSimilarity(Dream dream1, Dream dream2);
	
	/**
	 * Tekrarlayan rüyaları tespit eder (yüksek benzerlik skoruna sahip rüyalar)
	 * @param user Kullanıcı
	 * @param minSimilarity Minimum benzerlik skoru (varsayılan: 0.7)
	 * @return Tekrarlayan rüya grupları
	 */
	List<List<SimilarDreamDTO>> findRecurringDreams(User user, double minSimilarity);
}

