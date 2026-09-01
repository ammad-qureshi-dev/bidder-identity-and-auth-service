package dtos.response;

import java.util.UUID;

public record AppUserDto(UUID id, String fullName, PreferredContactMethod contact) {
}
