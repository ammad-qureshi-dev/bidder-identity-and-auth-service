/* (C) 2026
bidder.app */
package models;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "identity_and_auth_service", name = "app_user")
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseEntity implements UserDetails {

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

	public AppUser(UUID id, String email, String phoneNumber, String password) {
		this.id = id;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.password = password;
	}

	public AppUser(UUID id, UserDetails userDetails) {
		this.id = id;
		this.email = userDetails.getUsername();
		this.password = userDetails.getPassword();
	}

	public String getFirstName() {
		var spaceIndex = this.fullName.indexOf(' ');

		if (spaceIndex > 0) {
			return this.fullName.substring(0, spaceIndex);
		}

		return "";
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getUsername() {
		return email != null ? email : phoneNumber;
	}

	@Override
	public boolean isEnabled() {
		return verifiedAccount;
	}
}
