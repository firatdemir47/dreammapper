package com.dreammapper.mapper;

import com.dreammapper.dto.UserDTO;
import com.dreammapper.model.User;

public class UserMapper {

	public static UserDTO toDTO(User user) {
		if (user == null)
			return null;

		return UserDTO.builder().id(user.getId()).name(user.getName()).email(user.getEmail()).build();

	}

	public static User toEntity(UserDTO dto) {
		if (dto == null)
			return null;
		return User.builder().id(dto.getId()).name(dto.getName()).email(dto.getEmail()).build();
	}
}
