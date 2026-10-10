/* (C) 2026 
bidder.app */
package com.bidder.service.controllers;

import java.util.UUID;

import com.bidder.service.service.AuthService;
import com.bidder.service.service.JwtService;
import com.bidder.service.service.PasswordService;
import dtos.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final PasswordService passwordService;
	private final JwtService jwtService;

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<UUID>> login(@RequestBody LoginRequest request) throws BadCredentialsException {
		var response = authService.appUserLogin(request);
		var headers = jwtService.generateTokenCookieHeader(response.token());
		var loginResponse = ApiResponse.<UUID>builder().data(response.userId()).build();
		return new ResponseEntity<>(loginResponse, headers, HttpStatus.OK);
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UUID>> register(@RequestBody RegisterAppUserRequest request) {
		var appUser = authService.appUserRegistration(request);

		try {
			var recipientConfig = AuthService.retrieveFirstContactTypeFromRegistration(request);

			if (recipientConfig.isEmpty()) {
				log.error("No contact information provided, failed to send account verification link");
			} else {
				authService.sendAccountVerificationLink(appUser.userId());
			}
		} catch (Exception e) {
			log.error("Failed to send account verification link for user {}: ", appUser.userId(), e);
		}

		var headers = jwtService.generateTokenCookieHeader(appUser.token());
		var registerResponse = ApiResponse.<UUID>builder().data(appUser.userId()).build();
		return new ResponseEntity<>(registerResponse, headers, HttpStatus.OK);
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Boolean>> logout() {
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(),
				jwtService.clearTokenCookieHeader(), HttpStatus.OK);
	}

	@PostMapping("/password/reset/send-link")
	public ResponseEntity<ApiResponse<Boolean>> sendPasswordResetLink(@RequestBody PasswordResetLinkRequest request)
			throws IllegalStateException {

		passwordService.sendPasswordResetLink(request);
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(), HttpStatus.OK);
	}

	@PutMapping("/password/reset/set-password/{token}")
	public ResponseEntity<ApiResponse<Boolean>> setPassword(@PathVariable("token") String token,
			@RequestBody TokenRequest request) throws IllegalAccessException {
		passwordService.resetPassword(token, request);
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(), HttpStatus.OK);
	}

	@PostMapping("/account-verification/send-link")
	public ResponseEntity<ApiResponse<Boolean>> sendAccountVerificationLink(
			@RequestHeader("X-App-User-Id") UUID appUserId) throws IllegalAccessException {
		authService.sendAccountVerificationLink(appUserId);
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(), HttpStatus.OK);
	}

	@PutMapping("/account-verification/verify")
	public ResponseEntity<ApiResponse<Boolean>> verifyAccount(@RequestBody VerifyAccountRequest request)
			throws IllegalAccessException {
		authService.verifyAccount(request.token());
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(), HttpStatus.OK);
	}

	@GetMapping("/account-verification/is-verified")
	public ResponseEntity<ApiResponse<Boolean>> isVerified(@RequestHeader("X-App-User-Id") UUID appUserId) {
		var result = authService.isAccountVerified(appUserId);
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(result).build(), HttpStatus.OK);
	}
}
