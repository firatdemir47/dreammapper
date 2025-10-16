package com.dreammapper.mapper;

import com.dreammapper.dto.DreamDTO;
import com.dreammapper.model.Dream;
import com.dreammapper.model.User;

public class DreamMapper {

	public static DreamDTO toDTO(Dream dream) {
		if (dream == null)
			return null;

		return DreamDTO.builder().id(dream.getId()).description(dream.getDescription()).mood(dream.getMood())
				.createdAt(dream.getCreatedAt()).userId(dream.getUser() != null ? dream.getUser().getId() : null)
				.build();
	}

	public static Dream toEntity(DreamDTO dto, User user) {
		if (dto == null)
			return null;

		return Dream.builder().id(dto.getId()).description(dto.getDescription()).mood(dto.getMood())
				.createdAt(dto.getCreatedAt()).user(user).build();
	}
}
