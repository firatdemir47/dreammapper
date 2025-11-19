package com.dreammapper.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.dreammapper.dto.DreamDTO;
import com.dreammapper.dto.DreamExportDTO;
import com.dreammapper.mapper.DreamMapper;
import com.dreammapper.model.Dream;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.service.DreamService;
import com.dreammapper.service.ImportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportServiceImpl implements ImportService {

	private final DreamService dreamService;
	private final DreamRepository dreamRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public ImportResult importDreamsFromJson(String jsonData, User user) {
		List<String> errors = new ArrayList<>();
		int successCount = 0;
		int errorCount = 0;

		try {
			// JSON'u parse et
			List<DreamExportDTO> exportDTOs;
			try {
				exportDTOs = objectMapper.readValue(jsonData, new TypeReference<List<DreamExportDTO>>() {});
			} catch (Exception e) {
				// Tek bir rüya olabilir
				try {
					DreamExportDTO single = objectMapper.readValue(jsonData, DreamExportDTO.class);
					exportDTOs = List.of(single);
				} catch (Exception e2) {
					errors.add("Geçersiz JSON formatı: " + e.getMessage());
					return new ImportResult(0, 0, errors);
				}
			}

			if (exportDTOs == null || exportDTOs.isEmpty()) {
				errors.add("Import edilecek rüya bulunamadı");
				return new ImportResult(0, 0, errors);
			}

			// Her rüyayı import et
			for (int i = 0; i < exportDTOs.size(); i++) {
				DreamExportDTO exportDTO = exportDTOs.get(i);
				try {
					// DreamDTO'ya dönüştür
					DreamDTO dreamDTO = convertToDreamDTO(exportDTO);
					
					// Validation
					if (dreamDTO.getDescription() == null || dreamDTO.getDescription().isBlank()) {
						errors.add("Rüya " + (i + 1) + ": Açıklama boş olamaz");
						errorCount++;
						continue;
					}

					// Dream entity oluştur
					Dream dream = DreamMapper.toEntity(dreamDTO, user);
					
					// ID'yi null yap (yeni rüya olarak kaydedilecek)
					dream.setId(null);
					
					// Kaydet
					dreamService.saveDream(dream);
					successCount++;
					
				} catch (Exception e) {
					log.error("Error importing dream {}: ", i + 1, e);
					errors.add("Rüya " + (i + 1) + ": " + e.getMessage());
					errorCount++;
				}
			}

		} catch (Exception e) {
			log.error("Error parsing import JSON: ", e);
			errors.add("JSON parse hatası: " + e.getMessage());
			errorCount++;
		}

		return new ImportResult(successCount, errorCount, errors);
	}

	private DreamDTO convertToDreamDTO(DreamExportDTO exportDTO) {
		DreamDTO dto = new DreamDTO();
		dto.setDescription(exportDTO.getDescription());
		dto.setMood(exportDTO.getMood());
		dto.setTagsText(exportDTO.getTagsText());
		dto.setFavorite(exportDTO.getFavorite());
		dto.setNotes(exportDTO.getNotes());
		// createdAt ve userId set edilmez, yeni rüya olarak kaydedilecek
		return dto;
	}
}

