package dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record RegisterAppUserRequest(@NotNull String fullName,

		@Email String email,

		String phoneNumber,

		@NotNull String password) {
}
