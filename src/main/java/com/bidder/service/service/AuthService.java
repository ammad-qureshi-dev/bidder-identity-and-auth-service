/* (C) 2026 
bidder.app */
package com.bidder.service.service;

import java.util.Map;
import java.util.UUID;

import com.bidder.service.mappers.AppUserMapper;
import com.bidder.service.repository.AccessTokenRepository;
import com.bidder.service.repository.AppUserRepository;
import com.bidder.service.utils.HashingUtil;
import config.EventTopics;
import dtos.request.LoginRequest;
import dtos.request.RegisterAppUserRequest;
import dtos.request.SendLinkMethodRequest;
import dtos.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.AppUser;
import models.ContactType;
import models.TemplateName;
import models.TokenType;
import models.dtos.request.SendNotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bidder.service.utils.Constants.ExceptionMessages.INVALID_CREDENTIALS;
import static com.bidder.service.utils.Constants.ExceptionMessages.INVALID_PASSWORD_FORMAT;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final AppUserRepository appUserRepository;
	private final AppUserService appUserService;
	private final JwtService jwtService;
	private final PasswordService passwordService;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final AccessTokenRepository accessTokenRepository;

	@Value("${client.url}")
	private String clientUrl;

	@Value("${token-expiry.account-verification}")
	private int accountVerificationExpiry;

	@Transactional
	public AuthResponse appUserRegistration(RegisterAppUserRequest request) {
		validateAppUserRequest(request);

		var user = AppUserMapper.requestToEntity(request);

		appUserRepository.save(user);

		var token = jwtService.generateToken(user);

		var notificationRequest = new SendNotificationRequest(user.getId(), TemplateName.WELCOME_REGISTRATION,
				retrieveFirstContactTypeFromRegistration(request),
				Map.of("fullName", user.getFullName(), "appUrl", clientUrl));
		kafkaTemplate.send("notification", notificationRequest);

		return new AuthResponse(token, user.getId());
	}

	public AuthResponse appUserLogin(LoginRequest request) throws AuthenticationException {
		AppUser appUser;

		if (request.email() != null) {
			var userByEmail = appUserRepository.findByEmail(request.email());
			if (userByEmail.isEmpty()) {
				throw new BadCredentialsException(INVALID_CREDENTIALS);
			}
			appUser = userByEmail.get();
		} else {
			var userByPhone = appUserRepository.findByPhoneNumber(request.phoneNumber());
			if (userByPhone.isEmpty()) {
				throw new BadCredentialsException(INVALID_CREDENTIALS);
			}
			appUser = userByPhone.get();
		}

		if (!PasswordService.passwordsMatch(appUser.getPassword(), request.password())) {
			throw new BadCredentialsException(INVALID_CREDENTIALS);
		}

		var token = jwtService.generateToken(appUser);

		return new AuthResponse(token, appUser.getId());
	}

	public void sendAccountVerificationLink(SendLinkMethodRequest request, UUID appUserId) {
		var appUserResult = appUserRepository.findById(appUserId);

		if (appUserResult.isEmpty()) {
			log.info("No app-user with id = {} exists", appUserId);
			return;
		}

		var appUser = appUserResult.get();

		if (appUser.isVerifiedAccount()) {
			return;
		}

		Map<ContactType, String> recipientConfig;

		if (ContactType.EMAIL.equals(request.contactType())) {
			recipientConfig = Map.of(ContactType.EMAIL, appUser.getEmail());
		} else if (ContactType.PHONE.equals(request.contactType())) {
			recipientConfig = Map.of(ContactType.PHONE, appUser.getEmail());
		} else {
			throw new IllegalStateException("Contact type not supported");
		}

		var token = passwordService.generateAndSaveToken(appUser, TokenType.VERIFICATION, accountVerificationExpiry);

		if (token.isEmpty()) {
			log.info("Account Verification Token already exists for app-user-id = {}", appUserId);
			return;
		}

		var verificationUrl = clientUrl + "/verify-account?token=" + token.get();

		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new SendNotificationRequest(appUser.getId(), TemplateName.ACCOUNT_VERIFICATION, recipientConfig,
						Map.of("fullName", appUser.getFullName(), "verificationUrl", verificationUrl)));
	}

	@Transactional
	public void verifyAccount(String token) throws IllegalAccessException {
		if (token == null) {
			throw new IllegalAccessException("No account verification request found");
		}

		var hashedToken = HashingUtil.generateHash(token);
		var accessToken = accessTokenRepository.findByToken(hashedToken, TokenType.VERIFICATION);
		if (accessToken.isEmpty()) {
			log.error("Token provided but not found for type = {}", TokenType.VERIFICATION);
			throw new IllegalAccessException("No account verification request found");
		}

		var storedToken = accessToken.get();
		if (storedToken.isExpired()) {
			log.error("Verification token expired for app-user-id = {}", storedToken.getAppUser().getId());
			throw new IllegalAccessException("Link Expired");
		}

		var appUser = storedToken.getAppUser();

		// Prevent unnecessary database transaction if already verified
		if (appUser.isVerifiedAccount()) {
			return;
		}

		appUser.setVerifiedAccount(true);
		appUserRepository.save(appUser);

		accessTokenRepository.delete(storedToken);
		log.info("Account verified for app-user-id = {}", appUser.getId());
	}

	public boolean isAccountVerified(UUID appUserId) {
		var appUser = appUserRepository.findById(appUserId);

		if (appUser.isEmpty()) {
			throw new RuntimeException("User not found");
		}

		return appUser.get().isVerifiedAccount();
	}

	private void validateAppUserRequest(RegisterAppUserRequest request) {
		if (request.email() == null && request.phoneNumber() == null) {
			throw new IllegalStateException("Provide either email or phone-number");
		}

		if (!passwordService.isValidPassword(request.password())) {
			throw new IllegalStateException(INVALID_PASSWORD_FORMAT);
		}

		if (request.email() != null && appUserRepository.emailExists(request.email())) {
			throw new IllegalStateException("User already exists with that email");
		}

		if (request.fullName() == null) {
			throw new IllegalStateException("Name required");
		}
	}

	private static String getUsernameForSecurity(Object request) {
		if (request instanceof RegisterAppUserRequest req) {
			return req.email() != null ? req.email() : req.phoneNumber();
		} else {
			var req = (LoginRequest) request;
			return req.email() != null ? req.email() : req.phoneNumber();
		}
	}

	private static Map<ContactType, String> retrieveFirstContactTypeFromRegistration(RegisterAppUserRequest request) {
		if (request == null) {
			log.error("Registration request not found. Cannot derive contact type from registration request");
			throw new RuntimeException("Request not found");
		}

		if (request.email() != null) {
			return Map.of(ContactType.EMAIL, request.email());
		} else if (request.phoneNumber() != null) {
			return Map.of(ContactType.PHONE, request.phoneNumber());
		}

		throw new RuntimeException("No contact type found from registration request");
	}
}
