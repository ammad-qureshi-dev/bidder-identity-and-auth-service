package dtos.request;

import models.ContactType;

public record PasswordResetLinkRequest(
		ContactType contactType
) {
}
