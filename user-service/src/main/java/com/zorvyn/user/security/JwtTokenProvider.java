package com.zorvyn.user.security;

import com.zorvyn.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public record GeneratedToken(String value, Instant expiresAt) {}

    private final Key signingKey;
    private final long expirationMinutes;

    public JwtTokenProvider(
        @Value("${app.jwt.secret}") String base64Secret,
        @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(base64Secret)
        );
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user) {
        return generateTokenWithExpiry(user).value();
    }

    public GeneratedToken generateTokenWithExpiry(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
            expirationMinutes,
            ChronoUnit.MINUTES
        );

        String token = Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();

        return new GeneratedToken(token, expiresAt);
    }

    public String extractUsername(String token) {
        return extractUsername(parseAndValidateClaims(token));
    }

    public Instant getExpirationInstant(String token) {
        Date expiration = parseAndValidateClaims(token).getExpiration();
        return expiration.toInstant();
    }

    public boolean validateToken(String token) {
        parseAndValidateClaims(token);
        return true;
    }

    public Claims parseAndValidateClaims(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }
}
