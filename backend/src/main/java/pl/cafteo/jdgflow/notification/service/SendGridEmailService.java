package pl.cafteo.jdgflow.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.cafteo.jdgflow.notification.config.SendGridProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SendGridEmailService implements EmailService {

    private final RestClient restClient;
    private final SendGridProperties properties;

    public SendGridEmailService(RestClient sendGridHttpClient, SendGridProperties properties) {
        this.restClient = sendGridHttpClient;
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public void send(String toEmail, String subject, String htmlBody) {
        if (!isConfigured()) {
            throw new EmailDeliveryException(
                    "Email niedostępny — skonfiguruj zmienną środowiskową SENDGRID_API_KEY");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("personalizations", List.of(Map.of(
                "to", List.of(Map.of("email", toEmail))
        )));
        body.put("from", Map.of(
                "email", properties.fromEmail(),
                "name", properties.fromName()
        ));
        body.put("subject", subject);
        body.put("content", List.of(Map.of(
                "type", "text/html",
                "value", htmlBody
        )));

        try {
            restClient.post()
                    .uri("/v3/mail/send")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent email '{}' to {}", subject, toEmail);
        } catch (RestClientException ex) {
            log.warn("SendGrid call failed: {}", ex.getMessage());
            throw new EmailDeliveryException("SendGrid odrzucił żądanie: " + ex.getMessage(), ex);
        }
    }
}
