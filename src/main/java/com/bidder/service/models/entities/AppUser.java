/* (C) 2026 
bidder.app */
package com.bidder.service.models.entities;

import java.util.UUID;

import com.bidder.service.utils.Constants;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import models.ContactType;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = Constants.Database.SCHEMA, name = "app_user")
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotNull private String fullName;

	@NotNull private String password;

	private String email;

	// ToDo: to be broken down
	private String phoneNumber;

	private boolean verifiedAccount;

	@Nullable @Enumerated(EnumType.STRING)
	private ContactType preferredContactMethod;

	public String getFirstName() {
		var spaceIndex = this.fullName.indexOf(' ');

		if (spaceIndex > 0) {
			return this.fullName.substring(0, spaceIndex);
		}

		return "";
	}
}
