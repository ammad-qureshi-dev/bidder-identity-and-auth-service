package dtos.request;

import jakarta.validation.constraints.NotNull;
import models.ContactType;

public record SendLinkMethodRequest(
        @NotNull ContactType contactType
        ) {
}
