package com.zorvyn.user.service.impl;

import com.zorvyn.user.dto.request.LoginRequest;
import com.zorvyn.user.dto.response.AuthResponse;
import com.zorvyn.user.entity.User;
import com.zorvyn.user.entity.UserStatus;
import com.zorvyn.user.exception.UnauthorizedException;
import com.zorvyn.user.mapper.UserMapper;
import com.zorvyn.user.repository.UserRepository;
import com.zorvyn.user.security.JwtTokenProvider;
import com.zorvyn.user.service.AuthService;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request
            .email()
            .trim()
            .toLowerCase(Locale.ROOT);
        User user = userRepository
            .findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
            .orElseThrow(() ->
                new UnauthorizedException("Invalid email or password")
            );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (
            !passwordEncoder.matches(request.password(), user.getPasswordHash())
        ) {
            throw new UnauthorizedException("Invalid email or password");
        }

        JwtTokenProvider.GeneratedToken generatedToken =
            jwtTokenProvider.generateTokenWithExpiry(user);
        String token = generatedToken.value();
        Instant expiresAt = generatedToken.expiresAt();

        return new AuthResponse(
            "Bearer",
            token,
            expiresAt,
            UserMapper.toResponse(user)
        );
    }
}
