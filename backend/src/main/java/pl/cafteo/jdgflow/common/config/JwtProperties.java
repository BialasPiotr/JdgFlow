package pl.cafteo.jdgflow.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jdgflow.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration ttl,
        @NotBlank String issuer
) {}
