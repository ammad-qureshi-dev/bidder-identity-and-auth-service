/* (C) 2026 
bidder.app */
package com.bidder.service.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.bidder.service.repository.AccessTokenRepository;
import com.bidder.service.repository.AppUserRepository;
import com.bidder.service.utils.HashingUtil;
import config.EventTopics;
import dtos.request.TokenRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.AccessToken;
import models.AppUser;
import models.ContactType;
import models.TemplateName;
import models.TokenType;
import models.dtos.request.SendNotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.bidder.service.utils.Constants.Auth.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

	@Value("${client.url}")
	private String clientUrl;

	@Value("${auth.registration-pwd.allow-simple}")
	private boolean allowSimplePassword;

	@Value("${token-expiry.password-reset}")
	private int passwordResetExpiry;

	@Value("${token-expiry.account-verification}")
	private int accountVerificationExpiry;

	private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
	private static final Pattern PATTERN = Pattern.compile(PASSWORD_REGEX);

	private final AppUserService appUserService;
	private final AppUserRepository appUserRepository;
	private final AccessTokenRepository accessTokenRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public void sendPasswordResetLinkV2(UUID appUserId, ContactType contactType) {
		// ToDo: find a better way lol
		Map<ContactType, String> recipientConfig;

		var appUser = appUserRepository.findById(appUserId);

		if (appUser.isEmpty()) {
			log.error("Could not generate a Password Reset Link, app-user with id = {} not found", appUserId);
			return;
		}

		if (ContactType.PHONE.equals(contactType)) {
			recipientConfig = Map.of(contactType, appUser.get().getPhoneNumber());
		} else if (ContactType.EMAIL.equals(contactType)) {
			recipientConfig = Map.of(contactType, appUser.get().getEmail());
		} else {
			throw new RuntimeException("Contact Type not implemented");
		}

		var token = generateAndSaveToken(appUser.get(), TokenType.PASSWORD_RESET, passwordResetExpiry);

		if (token.isEmpty()) {
			log.info("Password Reset Token already exists for app-user with id = {}", appUserId);
			return;
		}

		var resetUrl = clientUrl + "/password/reset?token=" + token.get();
		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new SendNotificationRequest(appUserId, TemplateName.PASSWORD_RESET_LINK_SENT, recipientConfig,
						Map.of("fullName", appUser.get().getFullName(), "expiryMinutes", passwordResetExpiry / 60,
								"resetUrl", resetUrl)));
	}

	public void sendPasswordResetLink(TokenRequest request) {

		if (ContactType.EMAIL.equals(request.contactMethod())) {
			sendPasswordLinkByEmail(request.email());
		} else if (ContactType.PHONE.equals(request.contactMethod())) {
			throw new RuntimeException("Password Reset by mobile not implemented");
		} else {
			throw new RuntimeException("Method selected does not exist");
		}
	}

	public void resetPassword(String token, TokenRequest request) throws IllegalAccessException {
		var hashedToken = HashingUtil.generateHash(token);
		AppUser appUser = getAppUserByContactMethod(request);

		var passwordResetToken = accessTokenRepository.findByToken(hashedToken, TokenType.PASSWORD_RESET);
		validateClientToken(passwordResetToken, token, TokenType.PASSWORD_RESET);

		var hashedPassword = HashingUtil.generateHash(request.password());
		appUser.setPassword(hashedPassword);
		appUserRepository.save(appUser);
		log.info("Password successfully reset for app-user-id = {}", appUser.getId());

		passwordResetToken.ifPresent(accessTokenRepository::delete);

		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new SendNotificationRequest(appUser.getId(), TemplateName.PASSWORD_UPDATED,
						appUserService.getPreferredContactType(appUser), Map.of("fullName", appUser.getFullName())));
	}

	public boolean isValidPassword(String password) {
		if (allowSimplePassword) {
			return true;
		}

		if (password == null) {
			return false;
		}

		return PATTERN.matcher(password).matches();
	}

	public static boolean passwordsMatch(String basePwd, String inputPwd) {
		var hashedInputPwd = HashingUtil.generateHash(inputPwd);
		return hashedInputPwd.equals(basePwd);
	}

	private void sendPasswordLinkByEmail(@NotNull String email) {
		var appUser = appUserRepository.findByEmail(email);

		if (appUser.isEmpty()) {
			return;
		}

		var token = generateAndSaveToken(appUser.get(), TokenType.PASSWORD_RESET, passwordResetExpiry);

		if (token.isEmpty()) {
			log.info("Password Reset Token already exists for {}", email);
			return;
		}

		var resetUrl = clientUrl + "/password/reset?token=" + token.get();
		var notificationMessage = String.format(PASSWORD_RESET_MESSAGE, resetUrl);

		// ToDo: kafka event for password_reset
	}

	/**
	 * Generates and saves a hashed token for the user. If a token already exists,
	 * it will return nothing. A new token will return a value
	 *
	 * @param appUser
	 * @return token if newly created, return nothing if a token already exists and
	 *         is unexpired
	 */
	public Optional<String> generateAndSaveToken(AppUser appUser, TokenType tokenType, int expirySeconds) {
		var currentToken = accessTokenRepository.findByAppUserId(appUser.getId(), tokenType);

		if (currentToken.isPresent()) {
			var token = currentToken.get();

			// If the current token is expired, delete and create a new one
			if (token.isExpired()) {
				accessTokenRepository.delete(token);
			} else {
				return Optional.empty();
			}
		}

		var token = HashingUtil.generateToken();
		var passwordResetToken = AccessToken.builder().appUser(appUser).token(HashingUtil.generateHash(token))
				.expiresAt(Instant.now().plusSeconds(expirySeconds)).tokenType(tokenType).build();
		accessTokenRepository.save(passwordResetToken);
		return Optional.of(token);
	}

	private AppUser getAppUserByContactMethod(TokenRequest request) {
		if (ContactType.EMAIL.equals(request.contactMethod())) {
			return appUserService.getAppUserByEmail(request.email());
		} else if (ContactType.PHONE.equals(request.contactMethod())) {
			return appUserService.getAppUserByPhoneNumber(request.phoneNumber());
		} else {
			throw new RuntimeException("Method selected does not exist, user not found");
		}
	}

	private void validateClientToken(Optional<AccessToken> storedToken, String clientToken, TokenType tokenType)
			throws IllegalAccessException {
		if (storedToken.isEmpty()) {
			log.error("Token not found for incoming token: {}", clientToken);
			throw new IllegalAccessException("Invalid Access");
		}

		var token = storedToken.get();
		if (token.isExpired()) {
			log.error("Stored token expired for incoming token: {}", clientToken);
			throw new IllegalAccessException("Link Expired");
		}

		var hashedIncomingToken = HashingUtil.generateHash(clientToken);
		if (!token.getToken().equals(hashedIncomingToken) || !token.getTokenType().equals(tokenType)) {
			log.error("Tokens don't match{}", clientToken);
			throw new IllegalAccessException("Invalid Token");
		}
	}
}
