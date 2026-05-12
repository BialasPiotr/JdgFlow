package pl.cafteo.jdgflow.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.auth.api.dto.AuthResponse;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.common.config.JwtProperties;
import pl.cafteo.jdgflow.common.security.TokenIssuer;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AuthResponseFactory {

    private final TokenIssuer tokenIssuer;
    private final JwtProperties jwtProperties;

    public AuthResponse build(User user) {
        String token = tokenIssuer.issueToken(user.getId(), user.getEmail());
        Instant expiresAt = Instant.now().plus(jwtProperties.ttl());
        return new AuthResponse(
                token,
                expiresAt,
                new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getFullName()));
    }
}
