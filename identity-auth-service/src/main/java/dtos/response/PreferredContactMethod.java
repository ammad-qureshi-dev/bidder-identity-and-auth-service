package dtos.response;

import jakarta.validation.constraints.NotNull;
import models.ContactType;

import java.util.UUID;

public record PreferredContactMethod(
        ContactType type,
        String value,
        @NotNull UUID appUserId
) {
}
