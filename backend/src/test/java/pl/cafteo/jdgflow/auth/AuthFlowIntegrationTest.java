package pl.cafteo.jdgflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import pl.cafteo.jdgflow.AbstractIntegrationTest;
import pl.cafteo.jdgflow.auth.api.dto.AuthResponse;
import pl.cafteo.jdgflow.auth.api.dto.LoginRequest;
import pl.cafteo.jdgflow.auth.api.dto.RegisterRequest;
import pl.cafteo.jdgflow.auth.domain.UserRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Test
    void registers_user_then_logs_in_and_calls_protected_endpoint() {
        userRepository.deleteAll();

        RegisterRequest registerRequest = new RegisterRequest(
                "piotr@example.com", "secret-password-1234", "Piotr Białas");

        ResponseEntity<AuthResponse> registerResponse = rest.postForEntity(
                "/api/auth/register", registerRequest, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().token()).isNotBlank();

        ResponseEntity<AuthResponse> loginResponse = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest("piotr@example.com", "secret-password-1234"),
                AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> meResponse = rest.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody().get("email")).isEqualTo("piotr@example.com");
    }

    @Test
    void rejects_protected_endpoint_without_token() {
        ResponseEntity<String> response = rest.getForEntity("/api/auth/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejects_registration_when_email_missing_at_sign() throws Exception {
        userRepository.deleteAll();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("not-an-email", "long-password-1234", "Test"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_registration_when_email_missing_tld() throws Exception {
        userRepository.deleteAll();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@host", "long-password-1234", "Test"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_login_with_wrong_password() throws Exception {
        userRepository.deleteAll();

        rest.postForEntity("/api/auth/register",
                new RegisterRequest("test@example.com", "correct-password-12", "Test User"),
                AuthResponse.class);

        // MockMvc instead of TestRestTemplate — the latter cannot retry POST bodies
        // when Spring Security returns 401, due to Apache HttpClient streaming behavior.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "wrong-password-99"))))
                .andExpect(status().isUnauthorized());
    }
}
