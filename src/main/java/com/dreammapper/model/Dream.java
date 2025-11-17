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
@Table(name = "dreams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Dream {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	private String mood;

	// Comma-separated tags for simple filtering
	@Column(columnDefinition = "TEXT")
	private String tagsText;

	private Boolean favorite;

	// User notes/annotations for the dream
	@Column(columnDefinition = "TEXT")
	private String notes;

	private LocalDateTime createdAt;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@PrePersist
	public void prePersist() {
		createdAt = LocalDateTime.now();
		if (favorite == null) favorite = false;
	}
}
