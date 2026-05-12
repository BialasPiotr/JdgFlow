package pl.cafteo.jdgflow.integration.fakturownia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jdgflow.integration.fakturownia")
public record FakturowniaProperties(
        @NotBlank @Pattern(regexp = "^https?://.+$", message = "must be a full URL like https://your-account.fakturownia.pl")
        String baseUrl,
        @NotBlank String apiToken,
        int defaultPageSize
) {
    public FakturowniaProperties {
        if (defaultPageSize <= 0 || defaultPageSize > 100) {
            defaultPageSize = 50;
        }
    }
}
