package models;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVerification {
    private Instant lastRemindedOn;
    private Instant expirationDate;
    private boolean isVerified;

    @Enumerated(EnumType.STRING)
    private AccountReminderStatus status;

    private Instant verifiedOn;
}
