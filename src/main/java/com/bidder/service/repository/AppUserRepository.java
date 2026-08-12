/* (C) 2026 
bidder.app */
package com.bidder.service.repository;

import java.util.Optional;
import java.util.UUID;

import models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

	@Query("""
			    SELECT COUNT(u) > 0
			    FROM AppUser u
			    WHERE LOWER(u.email) = LOWER(:email)
			""")
	boolean emailExists(@Param("email") String email);

	Optional<AppUser> findByEmail(String email);
	Optional<AppUser> findByPhoneNumber(String phoneNumber);
}
