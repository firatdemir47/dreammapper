package com.dreammapper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;

@Repository
public interface DreamAnalysisRepository extends JpaRepository<DreamAnalysis, Long> {
    List<DreamAnalysis> findByDreamOrderByCreatedAtDesc(Dream dream);
}


