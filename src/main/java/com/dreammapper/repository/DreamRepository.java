package com.dreammapper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dreammapper.model.Dream;
import com.dreammapper.model.User;

@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {
	List<Dream> findByUser(User user);

}
