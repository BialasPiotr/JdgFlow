package pl.cafteo.jdgflow.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.notification.domain.Notification;
import pl.cafteo.jdgflow.notification.domain.NotificationRepository;
import pl.cafteo.jdgflow.notification.domain.NotificationType;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final ObligationEmailTemplate template;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean sendObligationReminder(User user, TaxObligation obligation, int daysBefore) {
        if (notificationRepository.existsDeliveredReminder(obligation.getId(), daysBefore)) {
            log.debug("Reminder already delivered for obligation={} window={}d, skipping",
                    obligation.getId(), daysBefore);
            return false;
        }

        ObligationEmailTemplate.Rendered rendered = template.renderReminder(obligation, daysBefore);
        return deliver(user.getId(), user.getEmail(),
                NotificationType.OBLIGATION_REMINDER, obligation.getId(), daysBefore, rendered);
    }

    @Transactional
    public Notification sendTest(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (!emailService.isConfigured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email nie skonfigurowany — uzupełnij ustawienia SMTP w profilu");
        }

        ObligationEmailTemplate.Rendered rendered = template.renderTest(user.getEmail());
        deliver(user.getId(), user.getEmail(),
                NotificationType.TEST, null, null, rendered);

        return notificationRepository.findByUserIdOrderBySentAtDesc(user.getId()).get(0);
    }

    private boolean deliver(UUID userId, String recipient,
                             NotificationType type, UUID obligationId, Integer daysBefore,
                             ObligationEmailTemplate.Rendered rendered) {
        try {
            emailService.send(recipient, rendered.subject(), rendered.htmlBody());
            notificationRepository.save(Notification.delivered(
                    userId, type, obligationId, daysBefore, recipient, rendered.subject()));
            return true;
        } catch (EmailService.EmailDeliveryException ex) {
            log.warn("Email delivery failed for user={} type={}: {}", userId, type, ex.getMessage());
            notificationRepository.save(Notification.failed(
                    userId, type, obligationId, daysBefore, recipient, rendered.subject(),
                    ex.getMessage()));
            if (type == NotificationType.TEST) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Wysyłka nie powiodła się: " + ex.getMessage());
            }
            return false;
        }
    }
}
