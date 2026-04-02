package com.zorvyn.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorvyn.common.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String POST_METHOD = "POST";

    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowSeconds;

    private final Map<String, Deque<Long>> requestBuckets =
        new ConcurrentHashMap<>();

    public LoginRateLimitFilter(
        ObjectMapper objectMapper,
        @Value(
            "${app.security.login-rate-limit.max-requests:20}"
        ) int maxRequests,
        @Value(
            "${app.security.login-rate-limit.window-seconds:60}"
        ) long windowSeconds
    ) {
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        Deque<Long> bucket = requestBuckets.computeIfAbsent(clientKey, key ->
            new ArrayDeque<>()
        );

        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst() <= windowStart) {
                bucket.pollFirst();
            }

            if (bucket.size() >= maxRequests) {
                writeRateLimitResponse(request, response);
                return;
            }

            bucket.offerLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return (
            POST_METHOD.equalsIgnoreCase(request.getMethod()) &&
            LOGIN_PATH.equals(request.getRequestURI())
        );
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "Too Many Requests",
            "RATE_LIMIT_EXCEEDED",
            "Too many login attempts. Please try again later.",
            request.getRequestURI(),
            List.of()
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
