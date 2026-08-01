/* (C) 2026 
bidder.app */
package com.bidder.service.controllers;

import java.util.Map;
import java.util.UUID;

import com.bidder.service.service.AppUserService;
import dtos.request.PreferredContactMethodRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.ContactType;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import static com.bidder.service.utils.Constants.Controller.*;

@RestController
@RequestMapping(BASE_URI_V1 + "/app-user")
@RequiredArgsConstructor
@Slf4j
public class AppUserController {

	private final AppUserService appUserService;

	@PutMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<Boolean>> updateContactType(@RequestBody PreferredContactMethodRequest request,
			@RequestHeader("X-App-User-Id") UUID bidderId) {

		appUserService.updatePreferredContactMethod(request, bidderId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(true).build());
	}

	@GetMapping("/contacts")
	public ResponseEntity<ApiResponse<Map<ContactType, String>>> getContactMethods(
			@RequestHeader("X-App-User-Id") UUID appUserId) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Map<ContactType, String>>builder()
				.data(appUserService.getContactMethods(appUserId)).build());
	}

	@GetMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<Pair<ContactType, String>>> getPreferredContactMethod(
			@RequestHeader("X-App-User-Id") UUID appUserId) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Pair<ContactType, String>>builder()
				.data(appUserService.getPreferredContactMethod(appUserId)).build());
	}

}
