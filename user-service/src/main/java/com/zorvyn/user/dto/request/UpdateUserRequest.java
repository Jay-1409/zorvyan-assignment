package com.zorvyn.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(
        min = 3,
        max = 50,
        message = "Username must be between 3 and 50 characters"
    )
    @Pattern(
        regexp = "^[a-zA-Z0-9_.-]+$",
        message = "Username can contain letters, numbers, underscore, dot, and hyphen"
    )
    String username,

    @Email(message = "Email must be valid") String email,

    @Size(
        min = 8,
        max = 100,
        message = "Password must be between 8 and 100 characters"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
        message = "Password must include uppercase, lowercase, number, and special character"
    )
    String password
) {}
