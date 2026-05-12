package pl.cafteo.jdgflow.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.cafteo.jdgflow.common.audit.AuditableEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Column(name = "obligation_id")
    private UUID obligationId;

    @Column(name = "days_before")
    private Integer daysBefore;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "delivered", nullable = false)
    private boolean delivered;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public static Notification delivered(UUID userId, NotificationType type, UUID obligationId,
                                          Integer daysBefore, String recipient, String subject) {
        Notification n = baseRecord(userId, type, obligationId, daysBefore, recipient, subject);
        n.delivered = true;
        return n;
    }

    public static Notification failed(UUID userId, NotificationType type, UUID obligationId,
                                       Integer daysBefore, String recipient, String subject,
                                       String errorMessage) {
        Notification n = baseRecord(userId, type, obligationId, daysBefore, recipient, subject);
        n.delivered = false;
        n.errorMessage = errorMessage;
        return n;
    }

    private static Notification baseRecord(UUID userId, NotificationType type, UUID obligationId,
                                            Integer daysBefore, String recipient, String subject) {
        Notification n = new Notification();
        n.id = UUID.randomUUID();
        n.userId = userId;
        n.type = type;
        n.obligationId = obligationId;
        n.daysBefore = daysBefore;
        n.recipient = recipient;
        n.subject = subject;
        n.sentAt = Instant.now();
        return n;
    }
}
