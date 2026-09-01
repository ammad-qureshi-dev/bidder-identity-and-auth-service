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
import models.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import static com.bidder.service.utils.Constants.Controller.*;

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
		var response = authService.appUserRegistration(request);
		var headers = jwtService.generateTokenCookieHeader(response.token());
		var loginResponse = ApiResponse.<UUID>builder().data(response.userId()).build();
		return new ResponseEntity<>(loginResponse, headers, HttpStatus.OK);
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Boolean>> logout() {
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(),
				jwtService.clearTokenCookieHeader(), HttpStatus.OK);
	}

	@PostMapping("/password/reset/send-link")
	public ResponseEntity<ApiResponse<Boolean>> sendPasswordResetLink(@RequestBody SendLinkMethodRequest request,
																	  @RequestHeader("X-App-User-Id") UUID appUserId) throws IllegalAccessException {

		passwordService.sendPasswordResetLinkV2(appUserId, request.contactType());
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(), HttpStatus.OK);
	}

	@PutMapping("/password/reset/set-password/{token}")
	public ResponseEntity<ApiResponse<Boolean>> setPassword(@PathVariable("token") String token,
			@RequestBody TokenRequest request) throws IllegalAccessException {
		passwordService.resetPassword(token, request);
		return new ResponseEntity<>(ApiResponse.<Boolean>builder().data(true).build(), HttpStatus.OK);
	}

	@PostMapping("/account-verification/send-link")
	public ResponseEntity<ApiResponse<Boolean>> sendAccountVerificationLink(@RequestBody SendLinkMethodRequest request,
			@RequestHeader("X-App-User-Id") UUID appUserId) throws IllegalAccessException {
		authService.sendAccountVerificationLink(request, appUserId);
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
