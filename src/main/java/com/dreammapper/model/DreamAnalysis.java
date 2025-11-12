package com.dreammapper.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dream_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DreamAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dream_id", nullable = false)
    private Dream dream;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    private String dominantEmotion;
    
    private String category;

    // Comma-separated list of symbols for simplicity
    @Column(columnDefinition = "TEXT")
    private String symbolsText;

    // Raw JSON string of scores (label -> score)
    @Column(columnDefinition = "TEXT")
    private String scoresJson;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}


