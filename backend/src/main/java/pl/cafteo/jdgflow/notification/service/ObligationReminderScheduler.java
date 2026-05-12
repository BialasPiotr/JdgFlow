package pl.cafteo.jdgflow.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.notification.config.SendGridProperties;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;
import pl.cafteo.jdgflow.tax.domain.TaxObligationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObligationReminderScheduler {

    private static final int DEFAULT_DAYS_BEFORE = 3;

    private final UserRepository userRepository;
    private final TaxObligationRepository obligationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SendGridProperties properties;

    @Scheduled(cron = "0 0 9 * * *")
    public void runDailyReminders() {
        if (!emailService.isConfigured()) {
            log.debug("SendGrid not configured, skipping reminder run");
            return;
        }

        int daysBefore = properties.reminderDaysBefore() != null
                ? properties.reminderDaysBefore()
                : DEFAULT_DAYS_BEFORE;
        LocalDate target = LocalDate.now().plusDays(daysBefore);

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return;

        Map<UUID, User> usersById = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        int sent = 0;
        for (User user : users) {
            List<TaxObligation> obligations = obligationRepository
                    .findByUserIdAndDeadlineBetweenOrderByDeadlineAsc(user.getId(), target, target);
            for (TaxObligation obligation : obligations) {
                if (obligation.isPaid()) continue;
                User owner = usersById.get(obligation.getUserId());
                if (owner == null) continue;
                try {
                    if (notificationService.sendObligationReminder(owner, obligation, daysBefore)) {
                        sent++;
                    }
                } catch (Exception ex) {
                    log.warn("Reminder for obligation {} failed: {}", obligation.getId(), ex.getMessage());
                }
            }
        }

        if (sent > 0) log.info("Reminder run: sent {} email(s) for deadline {}", sent, target);
        else          log.debug("Reminder run: no emails for deadline {}", target);
    }
}
