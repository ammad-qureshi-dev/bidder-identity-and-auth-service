/* (C) 2026 
bidder.app */
package models;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "identity_and_auth_service", name = "access_token")
@EqualsAndHashCode(callSuper = true)
public class AccessToken extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne
	private AppUser appUser;

	@NotNull private String token;

	@NotNull private LocalDateTime expiresAt;

	private LocalDateTime usedAt;

	@Enumerated(EnumType.STRING)
	private TokenType tokenType;

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(this.getExpiresAt());
	}
}
