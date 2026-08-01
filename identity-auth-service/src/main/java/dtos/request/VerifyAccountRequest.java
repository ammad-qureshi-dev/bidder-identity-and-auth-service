package dtos.request;

import jakarta.validation.constraints.NotNull;

public record VerifyAccountRequest(
		@NotNull String token
) {
}
