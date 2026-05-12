package pl.cafteo.jdgflow.common.security;

import java.util.UUID;

public interface TokenIssuer {
    String issueToken(UUID userId, String email);
}
