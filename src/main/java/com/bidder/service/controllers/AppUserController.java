/* (C) 2026 
bidder.app */
package com.bidder.service.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bidder.service.service.AppUserService;
import dtos.request.PreferredContactMethodRequest;
import dtos.response.AppUserDto;
import dtos.response.PreferredContactMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.ContactType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

@RestController
@RequestMapping("/api/v1/app-user")
@RequiredArgsConstructor
@Slf4j
public class AppUserController {

	private final AppUserService appUserService;

	// ToDo: [SECURITY CONCERN] -- X-App-User-Id is a client-supplied header with no
	// verification against an authenticated session/JWT (SecurityConfig permits all
	// requests). Any caller can set an arbitrary UUID in this header and modify
	// another
	// user's preferred contact method (broken access control / IDOR).
	@PutMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<Boolean>> updateContactType(@RequestBody PreferredContactMethodRequest request,
			@RequestHeader("X-App-User-Id") UUID appUserId) {

		appUserService.updatePreferredContactMethod(request, appUserId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(true).build());
	}

	// ToDo: [SECURITY CONCERN] -- X-App-User-Id is unverified/spoofable, allowing
	// any
	// caller to read another user's contact methods (PII disclosure via IDOR).
	@GetMapping("/contacts")
	public ResponseEntity<ApiResponse<Map<ContactType, String>>> getContactMethods(
			@RequestHeader("X-App-User-Id") UUID appUserId) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Map<ContactType, String>>builder()
				.data(appUserService.getContactMethods(appUserId)).build());
	}

	// ToDo: [SECURITY CONCERN] -- X-App-User-Id is unverified/spoofable, allowing
	// any
	// caller to read another user's preferred contact method (PII disclosure via
	// IDOR).
	@GetMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<PreferredContactMethod>> getPreferredContactMethod(
			@RequestHeader("X-App-User-Id") UUID appUserId) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<PreferredContactMethod>builder()
				.data(appUserService.getPreferredContactMethod(appUserId)).build());
	}

	// ToDo: [SECURITY CONCERN] -- this "internal" endpoint has no authentication at
	// all
	// (no token, no header check) and sits on the same public API path as the rest
	// of
	// the controller. Anyone who can reach the service can bulk-fetch PII (contact
	// methods) for arbitrary user IDs.
	// ToDo: [SECURITY CONCERN] -- appUserIds has no size limit, allowing a caller
	// to
	// pass an unbounded list and trigger an expensive bulk lookup (resource
	// exhaustion / DoS vector).
	@GetMapping("/internal/contacts/preferred-contact-methods")
	public ResponseEntity<ApiResponse<List<PreferredContactMethod>>> internalGetPreferredContactMethod(
			@RequestParam(required = true) List<UUID> appUserIds) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<PreferredContactMethod>>builder()
				.data(appUserService.internalGetPreferredContactMethods(appUserIds)).build());
	}

	// ToDo: [SECURITY CONCERN] -- no authentication or ownership check on this
	// endpoint. appUserId is an unauthenticated path variable, so anyone can fetch
	// any
	// other user's AppUserDto by guessing/enumerating UUIDs (IDOR / PII
	// disclosure).
	@GetMapping("/{appUserId}")
	public ResponseEntity<ApiResponse<AppUserDto>> getAppUser(@PathVariable UUID appUserId) {
		var user = appUserService.getAppUser(appUserId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<AppUserDto>builder().data(user).build());
	}
}
