/* (C) 2025 
bidder.app */
package com.bidder.service.configs;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

	// ToDo: [SECURITY CONCERN] -- anyRequest().permitAll() with no authentication
	// filter means every X-App-User-Id header consumed by controllers (e.g.
	// AppUserController, AuthController) is fully client-controlled and unverified.
	// This is a systemic authentication bypass: any caller can impersonate any user
	// by setting that header to an arbitrary UUID. Needs a filter that validates the
	// caller's JWT/session and derives the identity server-side instead of trusting
	// the header, plus an authorizeHttpRequests policy that isn't a blanket permitAll.
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(
						exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
							response.setContentType("application/json");
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						}).accessDeniedHandler((request, response, accessDeniedException) -> {
							response.setContentType("application/json");
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						}));

		return http.build();
	}

}
