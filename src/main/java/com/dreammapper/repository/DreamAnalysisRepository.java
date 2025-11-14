package com.dreammapper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.model.User;

@Repository
public interface DreamAnalysisRepository extends JpaRepository<DreamAnalysis, Long> {
    List<DreamAnalysis> findByDreamOrderByCreatedAtDesc(Dream dream);
    
    @Query("SELECT da FROM DreamAnalysis da WHERE da.dream.user = :user")
    List<DreamAnalysis> findByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(da) FROM DreamAnalysis da WHERE da.dream.user = :user")
    Long countByUser(@Param("user") User user);
}


