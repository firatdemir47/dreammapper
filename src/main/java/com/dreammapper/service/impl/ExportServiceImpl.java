package com.dreammapper.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dreammapper.dto.DreamExportDTO;
import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamAnalysisRepository;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.service.ExportService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final DreamRepository dreamRepository;
    private final DreamAnalysisRepository dreamAnalysisRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public ExportResult exportDreams(User user, String format) {
        List<Dream> dreams = dreamRepository.findByUser(user);
        List<DreamExportDTO> exportData = dreams.stream()
                .map(this::toExportDto)
                .collect(Collectors.toList());

        String normalizedFormat = format == null ? "json" : format.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedFormat) {
            case "csv" -> exportCsv(exportData);
            case "json" -> exportJson(exportData);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    private ExportResult exportJson(List<DreamExportDTO> exportData) {
        try {
            byte[] content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exportData);
            String filename = "dreams-" + FILE_TS.format(java.time.LocalDateTime.now()) + ".json";
            return new ExportResult(content, filename, "application/json");
        } catch (Exception e) {
            throw new RuntimeException("Failed to export dreams as JSON", e);
        }
    }

    private ExportResult exportCsv(List<DreamExportDTO> exportData) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,createdAt,mood,favorite,tags,notes,category,emotion,description\n");
        for (DreamExportDTO dto : exportData) {
            sb.append(dto.getId()).append(",");
            sb.append(quote(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "")).append(",");
            sb.append(quote(dto.getMood())).append(",");
            sb.append(dto.getFavorite() != null && dto.getFavorite() ? "true" : "false").append(",");
            sb.append(quote(dto.getTagsText())).append(",");
            sb.append(quote(dto.getNotes())).append(",");
            sb.append(quote(dto.getLastAnalysisCategory())).append(",");
            sb.append(quote(dto.getLastAnalysisEmotion())).append(",");
            sb.append(quote(dto.getDescription()));
            sb.append("\n");
        }
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "dreams-" + FILE_TS.format(java.time.LocalDateTime.now()) + ".csv";
        return new ExportResult(content, filename, "text/csv");
    }

    private String quote(String value) {
        if (value == null) {
            value = "";
        }
        String sanitized = value.replace("\"", "\"\"");
        return "\"" + sanitized + "\"";
    }

    private DreamExportDTO toExportDto(Dream dream) {
        DreamAnalysis latestAnalysis = dreamAnalysisRepository.findFirstByDreamOrderByCreatedAtDesc(dream);
        return DreamExportDTO.builder()
                .id(dream.getId())
                .description(dream.getDescription())
                .mood(dream.getMood())
                .tagsText(dream.getTagsText())
                .favorite(dream.getFavorite())
                .notes(dream.getNotes())
                .createdAt(dream.getCreatedAt())
                .lastAnalysisCategory(latestAnalysis != null ? latestAnalysis.getCategory() : null)
                .lastAnalysisEmotion(latestAnalysis != null ? latestAnalysis.getDominantEmotion() : null)
                .build();
    }
}

