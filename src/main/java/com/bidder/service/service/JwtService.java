/* (C) 2026 
bidder.app */
package com.bidder.service.service;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import models.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${cookies.secure}")
	private boolean cookiesSecure;

	@Value("${cookies.sameSite}")
	private String cookiesSameSite;

	@Value("${cookies.httpOnly}")
	private boolean httpOnly;

	public UUID extractAppUserId(String token) {
		String id = extractClaim(token, claims -> claims.get("id", String.class));
		return id != null ? UUID.fromString(id) : null;
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	public String generateToken(AppUser appUser) {
		var extraClaims = new HashMap<String, Object>();
		extraClaims.put("id", appUser.getId());
		return generateToken(extraClaims, appUser);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return Jwts.builder().setClaims(extraClaims).setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
				.signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
	}

	public ResponseCookie generateTokenCookie(String token) {
		return ResponseCookie.from("token", token).httpOnly(httpOnly).secure(cookiesSecure).path("/")
				.sameSite(cookiesSameSite).maxAge(Duration.ofHours(1)).build();
	}

	public HttpHeaders generateTokenCookieHeader(String token) {
		var headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, generateTokenCookie(token).toString());
		return headers;
	}

	public HttpHeaders clearTokenCookieHeader() {
		var expiredCookie = ResponseCookie.from("token", "").httpOnly(httpOnly).secure(cookiesSecure).path("/")
				.sameSite(cookiesSameSite).maxAge(Duration.ZERO).build();
		var headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, expiredCookie.toString());
		return headers;
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
	}

	private Key getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
