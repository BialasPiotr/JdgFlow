package pl.cafteo.jdgflow.auth.service;

import pl.cafteo.jdgflow.auth.api.dto.AuthResponse;
import pl.cafteo.jdgflow.auth.api.dto.RegisterRequest;

public interface RegistrationService {
    AuthResponse register(RegisterRequest request);
}
