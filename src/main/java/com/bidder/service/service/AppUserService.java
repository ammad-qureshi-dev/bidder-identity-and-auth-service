/* (C) 2026 
bidder.app */
package com.bidder.service.service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.bidder.service.models.entities.AppUser;
import com.bidder.service.repository.AppUserRepository;
import dtos.request.PreferredContactMethodRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.ContactType;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import static com.bidder.service.utils.Constants.Messages.NOT_APPLICABLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserService {

	private final AppUserRepository appUserRepository;

	public AppUser getAppUserById(UUID userId) {
		return appUserRepository.findById(userId).orElseThrow();
	}

	public AppUser getAppUserByEmail(@NotNull String email) {
		return appUserRepository.findByEmail(email).orElseThrow();
	}

	public AppUser getAppUserByPhoneNumber(String phoneNumber) {
		return appUserRepository.findByPhoneNumber(phoneNumber).orElseThrow();
	}

	public Map<ContactType, String> getPreferredContactType(AppUser appUser) {
		if (appUser == null) {
			throw new RuntimeException("AppUser is null, cannot access preferred contact type");
		}

		var type = appUser.getPreferredContactMethod();

		if (type == null) {
			log.error("No Preferred Contact Type found for this user = {}", appUser.getId());
			throw new IllegalStateException("Please set up a preferred contact here: http://...");
		}

		if (ContactType.EMAIL.equals(type)) {
			return Map.of(type, appUser.getEmail());
		}

		return Map.of(type, appUser.getPhoneNumber());
	}

	public void updatePreferredContactMethod(PreferredContactMethodRequest request, UUID appUserId) {
		var appUser = getAppUserById(appUserId);

		var contactType = request.contactType();
		appUser.setPreferredContactMethod(contactType);

		var contact = request.contact();
		if (contactType.equals(ContactType.EMAIL)) {
			if (appUser.getEmail() == null && contact == null) {
				throw new IllegalStateException("Please provide an email");
			}

			if (appUser.getEmail() == null) {
				appUser.setEmail(contact);
			}
		} else if (contactType.equals(ContactType.PHONE)) {
			if (appUser.getPhoneNumber() == null && contact == null) {
				throw new IllegalStateException("Please provide a phone number");
			}

			if (appUser.getPhoneNumber() == null) {
				appUser.setPhoneNumber(contact);
			}
		} else {
			throw new RuntimeException("That contact type does not exist");
		}

		appUserRepository.save(appUser);
	}

	public Map<ContactType, String> getContactMethods(UUID userId) {
		var appUser = getAppUserById(userId);

		return Map.of(ContactType.EMAIL, appUser.getEmail() != null ? appUser.getEmail() : NOT_APPLICABLE,
				ContactType.PHONE, appUser.getPhoneNumber() != null ? appUser.getPhoneNumber() : NOT_APPLICABLE);
	}

	public Pair<ContactType, String> getPreferredContactMethod(UUID userId) {
		var appUser = getAppUserById(userId);
		var preferred = appUser.getPreferredContactMethod();

		if (ContactType.EMAIL.equals(preferred)) {
			return Pair.of(ContactType.EMAIL, appUser.getEmail() != null ? appUser.getEmail() : NOT_APPLICABLE);
		} else if (ContactType.PHONE.equals(preferred)) {
			return Pair.of(ContactType.PHONE,
					appUser.getPhoneNumber() != null ? appUser.getPhoneNumber() : NOT_APPLICABLE);
		}

		// ToDo: append client URL to redirect user to add contact method
		throw new NoSuchElementException("No preferred contact method set. Please set a preferred contact method");

	}
}
