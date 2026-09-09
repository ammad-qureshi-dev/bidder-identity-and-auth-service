package dtos.request;

import jakarta.validation.constraints.NotNull;

public record PasswordResetLinkRequest(
        @NotNull String contact
) {
}
