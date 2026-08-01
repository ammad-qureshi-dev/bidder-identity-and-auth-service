/* (C) 2026 
bidder.app */
package com.bidder.service.mappers;

import com.bidder.service.models.entities.AppUser;
import com.bidder.service.utils.HashingUtil;
import dtos.request.RegisterAppUserRequest;
import dtos.response.AppUserDto;

public class AppUserMapper {
	public static final AppUser requestToEntity(RegisterAppUserRequest request) {
		return AppUser.builder().email(request.email()).fullName(request.fullName())
				.password(HashingUtil.generateHash(request.password())).phoneNumber(request.phoneNumber()).build();
	}

	public static AppUserDto entityToRequest(AppUser entity) {

		if (entity == null) {
			return null;
		}

		return new AppUserDto(entity.getId(), entity.getFullName(), entity.getPhoneNumber(), entity.getEmail());
	}
}
