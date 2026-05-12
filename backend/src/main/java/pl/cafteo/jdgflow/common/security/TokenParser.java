package pl.cafteo.jdgflow.common.security;

import java.util.Optional;

public interface TokenParser {
    Optional<TokenClaims> parse(String token);
}
