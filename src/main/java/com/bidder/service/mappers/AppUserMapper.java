/* (C) 2026 
bidder.app */
package com.bidder.service.mappers;

import com.bidder.service.utils.HashingUtil;
import dtos.request.RegisterAppUserRequest;
import dtos.response.AppUserDto;
import dtos.response.PreferredContactMethod;
import models.AppUser;
import models.ContactType;

public class AppUserMapper {
	public static final AppUser requestToEntity(RegisterAppUserRequest request) {
		return AppUser.builder().email(request.email()).fullName(request.fullName())
				.password(HashingUtil.generateHash(request.password())).phoneNumber(request.phoneNumber()).build();
	}

	public static AppUserDto entityToRequest(AppUser entity) {

		if (entity == null) {
			return null;
		}

		PreferredContactMethod contact;
		var savedContactMethod = entity.getPreferredContactMethod();

		if (ContactType.PHONE.equals(savedContactMethod)) {
			contact = new PreferredContactMethod(savedContactMethod, entity.getPhoneNumber(), entity.getId());
		} else if (ContactType.EMAIL.equals(savedContactMethod)) {
			contact = new PreferredContactMethod(savedContactMethod, entity.getEmail(), entity.getId());
		} else {
			contact = new PreferredContactMethod(null, null, entity.getId());
		}

		return new AppUserDto(entity.getId(), entity.getFullName(), contact);
	}
}
