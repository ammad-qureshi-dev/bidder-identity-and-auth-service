/* (C) 2026 
bidder.app */
package com.bidder.service.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.bidder.service.mappers.AppUserMapper;
import com.bidder.service.repository.AccessTokenRepository;
import com.bidder.service.repository.AppUserRepository;
import com.bidder.service.utils.HashingUtil;
import config.EventTopics;
import dtos.request.LoginRequest;
import dtos.request.RegisterAppUserRequest;
import dtos.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.*;
import models.dtos.request.NotifyRequest;
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

	// 30 days
	private static long ACCOUNT_VERIFICATION_EXPIRATION_LENGTH_SECONDS = 2592000;

	@Transactional
	public AuthResponse appUserRegistration(RegisterAppUserRequest request) {
		validateAppUserRequest(request);

		var user = AppUserMapper.requestToEntity(request);
		var now = Instant.now();
		user.setVerificationData(AccountVerification.builder().isVerified(false).lastRemindedOn(now)
				.expirationDate(now.plusSeconds(ACCOUNT_VERIFICATION_EXPIRATION_LENGTH_SECONDS)).status(null).build());

		appUserRepository.save(user);

		var token = jwtService.generateToken(user);
		var recipientConfig = retrieveFirstContactTypeFromRegistration(request);

		var notificationRequest = new NotifyRequest(user.getId(), TemplateName.WELCOME_REGISTRATION, recipientConfig,
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

	/**
	 * Sends an account verification link if and only if the given {@link AppUser}
	 * is not yet verified. Delegates to
	 * {@link PasswordService#generateAndSaveToken} to generate and persist the
	 * verification token.
	 */
	public void sendAccountVerificationLink(UUID appUserId) {
		var appUser = appUserService.getAppUserById(appUserId);
		var verificationData = appUser.getVerificationData();

		if (verificationData.isVerified()) {
			log.info("app-user-id = {} is already verified", appUser.getId());
			return;
		}

		if (appUser.getPreferredContactMethod() == null
				&& (appUser.getEmail() == null && appUser.getPhoneNumber() == null)) {
			throw new IllegalStateException(
					"Please set up an email or phone number to receive important and timely notifications");
		}

		var token = passwordService.generateAndSaveToken(appUser, TokenType.VERIFICATION, accountVerificationExpiry);

		if (token.isEmpty()) {
			log.info("Account Verification Token already exists for app-user-id = {}", appUserId);
			return;
		}

		var verificationUrl = clientUrl + "/verify-account?token=" + token.get();

		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new NotifyRequest(appUser.getId(), TemplateName.ACCOUNT_VERIFICATION, null,
						Map.of("fullName", appUser.getFullName(), "verificationUrl", verificationUrl)));
	}

	@Transactional(rollbackFor = java.lang.Exception.class)
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
		var verificationData = appUser.getVerificationData();

		// Prevent unnecessary database transaction if already verified
		if (verificationData.isVerified()) {
			return;
		}

		verificationData.setVerified(true);
		verificationData.setVerifiedOn(Instant.now());
		appUserRepository.save(appUser);

		accessTokenRepository.delete(storedToken);
		log.info("Account verified for app-user-id = {}", appUser.getId());
	}

	public boolean isAccountVerified(UUID appUserId) {
		var appUser = appUserRepository.findById(appUserId);

		if (appUser.isEmpty()) {
			throw new RuntimeException("User not found");
		}

		var data = appUser.get().getVerificationData();
		if (data == null) {
			return false;
		}

		return data.isVerified();
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

	public static Map<ContactType, String> retrieveFirstContactTypeFromRegistration(RegisterAppUserRequest request) {
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

	@Transactional
	public void updateVerificationStatus(AppUser appUser) {
		var verificationData = appUser.getVerificationData();
		var status = verificationData.getStatus();
		if (status == null) {
			verificationData.setStatus(AccountReminderStatus.INITIAL_REMINDER);
		} else if (AccountReminderStatus.INITIAL_REMINDER.equals(status)) {
			verificationData.setStatus(AccountReminderStatus.MID_REMINDER);
		} else if (AccountReminderStatus.MID_REMINDER.equals(status)) {
			verificationData.setStatus(AccountReminderStatus.FINAL_REMINDER);
		}
		verificationData.setLastRemindedOn(Instant.now());

		appUserRepository.save(appUser);
	}
}
