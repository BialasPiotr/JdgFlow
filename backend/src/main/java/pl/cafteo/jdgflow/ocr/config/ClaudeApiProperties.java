package pl.cafteo.jdgflow.ocr.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jdgflow.integration.claude")
public record ClaudeApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String model,
        Duration timeout
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("not-configured");
    }
}
