package com.zorvyn.finance.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(
            HttpHeaders.AUTHORIZATION
        );

        if (
            authorizationHeader != null &&
            authorizationHeader.startsWith(BEARER_PREFIX) &&
            SecurityContextHolder.getContext().getAuthentication() == null
        ) {
            String token = authorizationHeader.substring(
                BEARER_PREFIX.length()
            );

            try {
                Claims claims = jwtTokenProvider.parseAndValidateClaims(token);
                String subject = jwtTokenProvider.extractSubject(claims);
                String role = jwtTokenProvider.extractRole(claims);

                if (role != null && subject != null) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                            subject,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                    authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(
                            request
                        )
                    );
                    SecurityContextHolder.getContext().setAuthentication(
                        authenticationToken
                    );
                }
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute("authError", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
