package pl.cafteo.jdgflow.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String token,
        Instant expiresAt,
        UserSummary user
) {
    public record UserSummary(UUID id, String email, String fullName) {}
}
