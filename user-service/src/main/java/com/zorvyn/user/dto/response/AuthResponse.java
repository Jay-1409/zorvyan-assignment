package com.zorvyn.user.dto.response;

import java.time.Instant;

public record AuthResponse(
    String tokenType,
    String accessToken,
    Instant expiresAt,
    UserResponse user
) {
}
