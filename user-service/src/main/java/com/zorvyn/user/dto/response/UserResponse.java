package com.zorvyn.user.dto.response;

import com.zorvyn.user.entity.Role;
import com.zorvyn.user.entity.UserStatus;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    Role role,
    UserStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
