package pl.cafteo.jdgflow.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.notification.domain.Notification;
import pl.cafteo.jdgflow.notification.domain.NotificationRepository;
import pl.cafteo.jdgflow.notification.domain.NotificationType;
import pl.cafteo.jdgflow.notification.service.EmailService;
import pl.cafteo.jdgflow.notification.service.NotificationService;
import pl.cafteo.jdgflow.notification.service.ObligationEmailTemplate;
import pl.cafteo.jdgflow.tax.domain.ObligationType;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationService}. Verifies idempotency, audit-on-failure,
 * and the SERVICE_UNAVAILABLE behaviour for the manual /test endpoint when SendGrid is down.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock EmailService emailService;
    @Mock ObligationEmailTemplate template;
    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks NotificationService service;

    @Test
    @DisplayName("sendObligationReminder — drugie wywołanie tej samej kombinacji obligation+window jest skip")
    void reminder_is_idempotent() {
        User user = stubUser();
        TaxObligation obligation = stubObligation(user.getId(), ObligationType.PIT_ADVANCE);
        when(notificationRepository.existsDeliveredReminder(obligation.getId(), 3)).thenReturn(true);

        boolean sent = service.sendObligationReminder(user, obligation, 3);

        assertThat(sent).isFalse();
        verify(emailService, never()).send(anyString(), anyString(), anyString());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendObligationReminder — happy path: wysyła email i loguje delivered=true")
    void reminder_sends_and_logs_delivery() {
        User user = stubUser();
        TaxObligation obligation = stubObligation(user.getId(), ObligationType.ZUS_SOCIAL);
        when(notificationRepository.existsDeliveredReminder(obligation.getId(), 3)).thenReturn(false);
        when(template.renderReminder(obligation, 3))
                .thenReturn(new ObligationEmailTemplate.Rendered("subject", "<p>body</p>"));

        boolean sent = service.sendObligationReminder(user, obligation, 3);

        assertThat(sent).isTrue();
        verify(emailService).send(user.getEmail(), "subject", "<p>body</p>");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.isDelivered()).isTrue();
        assertThat(saved.getType()).isEqualTo(NotificationType.OBLIGATION_REMINDER);
        assertThat(saved.getObligationId()).isEqualTo(obligation.getId());
        assertThat(saved.getDaysBefore()).isEqualTo(3);
        assertThat(saved.getRecipient()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("sendObligationReminder — wysyłka padła: zapisuje failed audit, NIE rzuca")
    void reminder_failure_is_audited_silently() {
        User user = stubUser();
        TaxObligation obligation = stubObligation(user.getId(), ObligationType.VAT);
        when(notificationRepository.existsDeliveredReminder(obligation.getId(), 3)).thenReturn(false);
        when(template.renderReminder(obligation, 3))
                .thenReturn(new ObligationEmailTemplate.Rendered("s", "b"));
        doThrow(new EmailService.EmailDeliveryException("SendGrid 401"))
                .when(emailService).send(anyString(), anyString(), anyString());

        boolean sent = service.sendObligationReminder(user, obligation, 3);

        assertThat(sent).isFalse();
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification audit = captor.getValue();
        assertThat(audit.isDelivered()).isFalse();
        assertThat(audit.getErrorMessage()).contains("SendGrid 401");
    }

    @Test
    @DisplayName("sendTest — gdy klucz nie skonfigurowany rzuca 503")
    void test_send_returns_503_when_unconfigured() {
        User user = stubUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(emailService.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.sendTest(user.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("sendTest — happy path: zapisuje delivered=true i zwraca najnowszy rekord")
    void test_send_returns_persisted_record() {
        User user = stubUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(emailService.isConfigured()).thenReturn(true);
        when(template.renderTest(user.getEmail()))
                .thenReturn(new ObligationEmailTemplate.Rendered("Test", "body"));

        Notification persisted = Notification.delivered(
                user.getId(), NotificationType.TEST, null, null, user.getEmail(), "Test");
        when(notificationRepository.findByUserIdOrderBySentAtDesc(user.getId()))
                .thenReturn(List.of(persisted));

        Notification result = service.sendTest(user.getId());

        verify(emailService).send(user.getEmail(), "Test", "body");
        assertThat(result).isSameAs(persisted);
    }

    @Test
    @DisplayName("sendTest — wysyłka padła: rzuca 503 z komunikatem (żeby user widział problem)")
    void test_send_propagates_failure_as_503() {
        User user = stubUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(emailService.isConfigured()).thenReturn(true);
        when(template.renderTest(anyString()))
                .thenReturn(new ObligationEmailTemplate.Rendered("s", "b"));
        doThrow(new EmailService.EmailDeliveryException("DNS fail"))
                .when(emailService).send(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.sendTest(user.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        // Audit row must still be persisted even though we re-throw.
        verify(notificationRepository).save(any(Notification.class));
    }

    private static User stubUser() {
        return User.register("piotr@example.com", "hash", "Piotr Białas");
    }

    private static TaxObligation stubObligation(UUID userId, ObligationType type) {
        return TaxObligation.create(
                UUID.randomUUID(), userId, type,
                new BigDecimal("1234.56"), LocalDate.now().plusDays(3));
    }
}
