/* (C) 2026 
bidder.app */
package com.bidder.service.models.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.bidder.service.utils.Constants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = Constants.Database.SCHEMA, name = "access_token")
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
