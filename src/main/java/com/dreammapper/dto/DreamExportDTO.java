package com.dreammapper.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DreamExportDTO {

    private Long id;
    private String description;
    private String mood;
    private String tagsText;
    private Boolean favorite;
    private String notes;
    private LocalDateTime createdAt;

    private String lastAnalysisCategory;
    private String lastAnalysisEmotion;
}

