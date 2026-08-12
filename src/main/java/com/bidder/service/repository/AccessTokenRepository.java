/* (C) 2026 
bidder.app */
package com.bidder.service.repository;

import java.util.Optional;
import java.util.UUID;

import models.AccessToken;
import models.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessTokenRepository extends JpaRepository<AccessToken, UUID> {

	@Query("""
			select AT
			from AccessToken AT
			where AT.token = :token
			and AT.tokenType = :tokenType
			""")
	Optional<AccessToken> findByToken(@Param("token") String token, @Param("tokenType") TokenType tokenType);

	@Query("""
			select AT
			from AccessToken AT
			where AT.appUser.id = :appUserId
			and AT.tokenType = :tokenType
			""")
	Optional<AccessToken> findByAppUserId(@Param("appUserId") UUID appUserId, @Param("tokenType") TokenType tokenType);

}
