package com.dreammapper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ChangePasswordDTO {

	@NotBlank(message = "Current password cannot be empty")
	private String currentPassword;

	@NotBlank(message = "New password cannot be empty")
	@Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
	private String newPassword;
}

