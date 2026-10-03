/* (C) 2026 
bidder.app */
package com.bidder.service.service;

import java.util.*;

import com.bidder.service.mappers.AppUserMapper;
import com.bidder.service.repository.AppUserRepository;
import dtos.request.PreferredContactMethodRequest;
import dtos.response.AppUserDto;
import dtos.response.PreferredContactMethod;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.AppUser;
import models.ContactType;
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

	public PreferredContactMethod getPreferredContactMethod(UUID userId) {
		var appUser = getAppUserById(userId);
		return getPreferredContactMethodHelper(appUser);

		// throw new NoSuchElementException("No preferred contact method set. Please set
		// a preferred contact method");
	}

	public List<PreferredContactMethod> internalGetPreferredContactMethods(List<UUID> appUserIds) {
		if (appUserIds == null || appUserIds.isEmpty()) {
			return Collections.emptyList();
		}

		var contactMethods = new ArrayList<PreferredContactMethod>();

		var appUsers = appUserRepository.findAllById(appUserIds);
		for (var appUser : appUsers) {
			contactMethods.add(getPreferredContactMethodHelper(appUser));
		}

		return contactMethods;
	}

	public AppUserDto getAppUser(UUID appUserId) {
		var appUser = getAppUserById(appUserId);
		return AppUserMapper.entityToRequest(appUser);
	}

	private static PreferredContactMethod getPreferredContactMethodHelper(AppUser appUser) {
		var preferred = appUser.getPreferredContactMethod();

		if (ContactType.EMAIL.equals(preferred)) {
			return new PreferredContactMethod(ContactType.EMAIL,
					appUser.getEmail() != null ? appUser.getEmail() : NOT_APPLICABLE, appUser.getId());
		} else if (ContactType.PHONE.equals(preferred)) {
			return new PreferredContactMethod(ContactType.PHONE,
					appUser.getPhoneNumber() != null ? appUser.getPhoneNumber() : NOT_APPLICABLE, appUser.getId());
		}

		return new PreferredContactMethod(ContactType.APP, "", appUser.getId());
	}
}
