package pl.cafteo.jdgflow.auth.service;

import pl.cafteo.jdgflow.auth.api.dto.AuthResponse;
import pl.cafteo.jdgflow.auth.api.dto.LoginRequest;

public interface LoginService {
    AuthResponse login(LoginRequest request);
}
