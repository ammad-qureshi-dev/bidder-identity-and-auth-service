/* (C) 2025 
bidder.app */
package com.bidder.service.configs;

import java.util.List;

import com.bidder.service.controllers.filters.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final AuthenticationProvider authenticationProvider;

	@Value("${client.url}")
	private String clientUrl;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfigurationSource())).csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/identity-and-auth/auth/**", "/swagger-ui/**", "/v3/api-docs/**",
								"/swagger-ui.html", "/v3/api-docs.yaml", "/swagger-resources/**")
						.permitAll().anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class).exceptionHandling(
						exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
							response.setContentType("application/json");
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setHeader("Access-Control-Allow-Origin", clientUrl);
							response.setHeader("Access-Control-Allow-Credentials", "true");
						}).accessDeniedHandler((request, response, accessDeniedException) -> {
							response.setContentType("application/json");
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setHeader("Access-Control-Allow-Origin", clientUrl);
							response.setHeader("Access-Control-Allow-Credentials", "true");
						}));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(clientUrl));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
		config.setAllowCredentials(true);
		config.setExposedHeaders(List.of("Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

}
