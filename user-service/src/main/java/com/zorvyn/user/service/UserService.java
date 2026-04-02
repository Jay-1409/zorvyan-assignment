package com.zorvyn.user.service;

import com.zorvyn.user.dto.request.CreateUserRequest;
import com.zorvyn.user.dto.request.UpdateUserRequest;
import com.zorvyn.common.dto.PagedResponse;
import com.zorvyn.user.dto.response.UserResponse;
import com.zorvyn.user.entity.Role;
import com.zorvyn.user.entity.UserStatus;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    PagedResponse<UserResponse> listUsers(
        Role role,
        UserStatus status,
        String search,
        int page,
        int size,
        String sortBy,
        String sortDir
    );

    UserResponse updateUser(Long id, UpdateUserRequest request);

    UserResponse updateRole(Long id, Role role);

    UserResponse updateStatus(Long id, UserStatus status);

    void deleteUser(Long id, String actorEmail);

    UserResponse getCurrentUser(String email);
}
