/* (C) 2026 
bidder.app */
package com.bidder.service.controllers;

import java.util.Map;

import com.bidder.service.service.AppUserService;
import dtos.request.PreferredContactMethodRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.AppUser;
import models.ContactType;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import static com.bidder.service.utils.Constants.Controller.*;

@RestController
@RequestMapping("/api/v1/app-user")
@RequiredArgsConstructor
@Slf4j
public class AppUserController {

	private final AppUserService appUserService;

	@PutMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<Boolean>> updateContactType(@RequestBody PreferredContactMethodRequest request,
			@AuthenticationPrincipal AppUser appUser) {

		appUserService.updatePreferredContactMethod(request, appUser.getId());
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(true).build());
	}

	@GetMapping("/contacts")
	public ResponseEntity<ApiResponse<Map<ContactType, String>>> getContactMethods(
			@AuthenticationPrincipal AppUser appUser) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Map<ContactType, String>>builder()
				.data(appUserService.getContactMethods(appUser.getId())).build());
	}

	@GetMapping("/contacts/preferred-contact-method")
	public ResponseEntity<ApiResponse<Pair<ContactType, String>>> getPreferredContactMethod(
			@AuthenticationPrincipal AppUser appUser) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Pair<ContactType, String>>builder()
				.data(appUserService.getPreferredContactMethod(appUser.getId())).build());
	}

}
