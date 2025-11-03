package com.dreammapper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dreammapper.model.Dream;
import com.dreammapper.model.User;

@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {
	List<Dream> findByUser(User user);

    @Query("SELECT d FROM Dream d WHERE (:q IS NULL OR LOWER(d.description) LIKE LOWER(CONCAT('%', :q, '%')))"
         + " AND (:tags IS NULL OR LOWER(COALESCE(d.tagsText, '')) LIKE LOWER(CONCAT('%', :tags, '%')))"
         + " AND (:fav IS NULL OR d.favorite = :fav)")
    List<Dream> search(@Param("q") String q,
                       @Param("tags") String tags,
                       @Param("fav") Boolean favorite);
}
