package pl.cafteo.jdgflow.common.security;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email) {}
