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

	@PutMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<Boolean>> updateContactType(@RequestBody PreferredContactMethodRequest request,
			@RequestHeader("X-App-User-Id") UUID appUserId) {

		appUserService.updatePreferredContactMethod(request, appUserId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(true).build());
	}

	@GetMapping("/contacts")
	public ResponseEntity<ApiResponse<Map<ContactType, String>>> getContactMethods(
			@RequestHeader("X-App-User-Id") UUID appUserId) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Map<ContactType, String>>builder()
				.data(appUserService.getContactMethods(appUserId)).build());
	}

	@GetMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<PreferredContactMethod>> getPreferredContactMethod(
			@RequestHeader("X-App-User-Id") UUID appUserId) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<PreferredContactMethod>builder()
				.data(appUserService.getPreferredContactMethod(appUserId)).build());
	}

	// ToDo: for any internal requests, add a token and validate token. Internal
	// endpoints can leak internal PII data
	@GetMapping("/internal/contacts/preferred-contact-methods")
	public ResponseEntity<ApiResponse<List<PreferredContactMethod>>> internalGetPreferredContactMethod(
			@RequestParam(required = true) List<UUID> appUserIds) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<PreferredContactMethod>>builder()
				.data(appUserService.internalGetPreferredContactMethods(appUserIds)).build());
	}

	@GetMapping("/{appUserId}")
	public ResponseEntity<ApiResponse<AppUserDto>> getAppUser(@PathVariable UUID appUserId) {
		var user = appUserService.getAppUser(appUserId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<AppUserDto>builder().data(user).build());
	}
}
