package com.zorvyn.user.service;

import com.zorvyn.user.dto.request.LoginRequest;
import com.zorvyn.user.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
