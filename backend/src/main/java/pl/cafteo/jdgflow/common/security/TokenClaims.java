package pl.cafteo.jdgflow.common.security;

import java.util.UUID;

public record TokenClaims(UUID userId, String email) {}
