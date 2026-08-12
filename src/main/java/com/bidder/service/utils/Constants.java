/* (C) 2026 
bidder.app */
package com.bidder.service.utils;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Constants {

	@AllArgsConstructor
	public static class Controller {
		public static final String BASE_URI = "/api/identity-and-auth";
		public static final String AUTH_BASE_URI = BASE_URI + "/v1/auth";
	}

	public static class Database {
		public static final String SCHEMA = "identity_and_auth_service";
	}

	public static class ExceptionMessages {
		public static final String INVALID_CREDENTIALS = "Invalid credentials provided";
		public static final String INVALID_PASSWORD_FORMAT = "Invalid password format";
	}

	public static class Messages {
		public static final String CONTACT_INFO_SETUP_TITLE = "Contact Info Setup";
		public static final String WELCOME_TITLE = "Welcome to Biddr";
		public static final String WELCOME_MESSAGE = "Thanks for signing up! Let's start bidding...";
		public static final String NOT_APPLICABLE = "N/A";
	}

	public static class Auth {
		public static final String TOKEN_COOKIE = "token";
		public static final String PASSWORD_RESET = "Password Reset Request";
		public static final String PASSWORD_RESET_MESSAGE = "You have requested to reset your password. Please open this link to securely reset your password: %s";
		public static final String VERIFY_ACCOUNT = "Account Verification Request";
		public static final String VERIFY_ACCOUNT_MESSAGE = "Hi %s, please click the link below to verify your account: %s";
	}
}
