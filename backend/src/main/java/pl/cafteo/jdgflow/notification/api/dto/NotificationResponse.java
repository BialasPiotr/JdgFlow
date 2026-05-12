package pl.cafteo.jdgflow.notification.api.dto;

import pl.cafteo.jdgflow.notification.domain.Notification;
import pl.cafteo.jdgflow.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID obligationId,
        Integer daysBefore,
        String recipient,
        String subject,
        Instant sentAt,
        boolean delivered,
        String errorMessage
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getObligationId(),
                n.getDaysBefore(),
                n.getRecipient(),
                n.getSubject(),
                n.getSentAt(),
                n.isDelivered(),
                n.getErrorMessage()
        );
    }
}
