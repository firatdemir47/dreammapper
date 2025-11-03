package com.dreammapper.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
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
public class DreamDTO {

	private Long id;

	@NotBlank(message = "Dream description cannot be empty")
	private String description;

	private String mood;

    private String tagsText;

    private Boolean favorite;

	private LocalDateTime createdAt;

	private Long userId;
}
