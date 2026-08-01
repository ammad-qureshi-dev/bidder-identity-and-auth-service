package dtos.response;

import java.util.UUID;

public record AppUserDto(UUID id, String fullName, String phoneNumber, String email) {
}
