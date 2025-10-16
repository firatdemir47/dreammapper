package com.dreammapper.dto;

import jakarta.validation.constraints.Email;
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
public class UserDTO {

	private Long id;

	@NotBlank(message = "Name cannot be empty")
	private String name;

	@Email(message = "Email must be valid")
	@NotBlank(message = "Email cannot be empty")
	private String email;

}
