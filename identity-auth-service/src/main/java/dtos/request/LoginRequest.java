package dtos.request;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(String email,

		String phoneNumber,

		@NotNull String password) {
}
