package pl.cafteo.jdgflow.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jdgflow.integration.sendgrid")
public record SendGridProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String fromEmail,
        @NotBlank String fromName,
        Duration timeout,
        Integer reminderDaysBefore
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("not-configured");
    }
}
