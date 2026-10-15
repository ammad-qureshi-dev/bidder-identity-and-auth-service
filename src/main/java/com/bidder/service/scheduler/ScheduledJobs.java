/* (C) 2026
bidder.app */
package com.bidder.service.scheduler;

import java.util.ArrayList;

import com.bidder.service.repository.AppUserRepository;
import com.bidder.service.service.AuthService;
import config.EventTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.AccountReminderStatus;
import models.AppUser;
import models.dtos.request.NotifyRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

	private final AppUserRepository appUserRepository;
	private final AuthService authService;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private static final String EVERY_15_MINUTES = "0 */15 * * * *";
	private static final int MID_REMINDER_DAYS = 3;
	private static final int FINAL_REMINDER_DAYS = 7;

	@Scheduled(cron = EVERY_15_MINUTES)
	public void sendVerificationRequest() {
		log.info("scheduled job [sendVerificationRequest] -- running");
		var unverifiedAccounts = new ArrayList<AppUser>();
		// users still on their initial reminder are due the mid reminder, users on the
		// mid reminder are due the final reminder
		var midReminderAccounts = appUserRepository
				.findUsersDueForReminder(AccountReminderStatus.INITIAL_REMINDER.name(), MID_REMINDER_DAYS);
		var finalReminderAccounts = appUserRepository
				.findUsersDueForReminder(AccountReminderStatus.MID_REMINDER.name(), FINAL_REMINDER_DAYS);

		unverifiedAccounts.addAll(midReminderAccounts);
		unverifiedAccounts.addAll(finalReminderAccounts);

		if (unverifiedAccounts.isEmpty()) {
			log.info("scheduled job [sendVerificationRequest] -- no accounts found");
			return;
		}

		log.info(
				"scheduled job [sendVerificationRequest] -- processing {} accounts for mid reminders, {} for final reminders. Total unverified accounts = {}",
				midReminderAccounts.size(), finalReminderAccounts.size(), unverifiedAccounts.size());
		unverifiedAccounts.forEach(e -> {
			try {
				authService.updateVerificationStatus(e);
				authService.sendAccountVerificationLink(e.getId());
			} catch (Exception ex) {
				log.error("scheduled job [sendVerificationRequest] -- failed for app-user-id = {}", e.getId(), ex);
			}
		});
		log.info("scheduled job [sendVerificationRequest] -- processed");
	}

	@Scheduled(cron = EVERY_15_MINUTES)
	public void deleteUsersPastExpiration() {
		log.info("scheduled job [deleteUsersPastExpiration] -- running");
		var usersPastExpiration = appUserRepository.findUsersPastExpiryDate();

		if (usersPastExpiration.isEmpty()) {
			log.info("scheduled job [deleteUsersPastExpiration] -- no accounts found");
			return;
		}

		log.info("scheduled job [deleteUsersPastExpiration] -- {} users found for deletion", usersPastExpiration.size());
		usersPastExpiration.forEach(e -> {
			try {
				kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(), new NotifyRequest(e.getId(), null, null, null));
				appUserRepository.delete(e);
			} catch (Exception ex) {
				log.error("scheduled job [deleteUsersPastExpiration] -- failed for app-user-id = {}", e.getId(), ex);
			}
		});

		log.info("scheduled job [deleteUsersPastExpiration] -- processed");
	}
}
