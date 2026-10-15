/* (C) 2026 
bidder.app */
package com.bidder.service.repository;

import java.util.List;
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

	@Query(value = """
			select *
			from identity_and_auth_service.app_user a
			where (a.verification_data->>'isVerified')::boolean = false
			  and a.verification_data->>'status' = :status
			  and (a.verification_data->>'lastRemindedOn')::timestamptz < now() - (:day * interval '1 day')
			  and (a.verification_data->>'expirationDate')::timestamptz > now()
			""", nativeQuery = true)
	List<AppUser> findUsersDueForReminder(@Param("status") String status, @Param("day") long day);

	@Query(value = """
			select *
			from identity_and_auth_service.app_user a
			where (a.verification_data->>'isVerified')::boolean = false
			  and a.verification_data->>'status' = 'FINAL_REMINDER'
			  and (a.verification_data->>'expirationDate')::timestamptz <= now()
			""", nativeQuery = true)
	List<AppUser> findUsersPastExpiryDate();
}
