package pl.cafteo.jdgflow.notification.service;

public interface EmailService {

    void send(String toEmail, String subject, String htmlBody);

    boolean isConfigured();

    class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message) { super(message); }
        public EmailDeliveryException(String message, Throwable cause) { super(message, cause); }
    }
}
