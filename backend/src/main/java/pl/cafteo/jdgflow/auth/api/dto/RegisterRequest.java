package pl.cafteo.jdgflow.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Email musi zawierać @ oraz prawidłową domenę"
        )
        @Size(max = 255)
        String email,

        @NotBlank @Size(min = 12, max = 128) String password,

        @NotBlank @Size(max = 255) String fullName
) {}
