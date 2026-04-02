package com.zorvyn.finance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final Key signingKey;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String base64Secret) {
        this.signingKey = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(base64Secret)
        );
    }

    public boolean validateToken(String token) {
        parseAndValidateClaims(token);
        return true;
    }

    public String extractSubject(String token) {
        return extractSubject(parseAndValidateClaims(token));
    }

    public String extractRole(String token) {
        Object role = parseAndValidateClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    public Claims parseAndValidateClaims(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractSubject(Claims claims) {
        return claims.getSubject();
    }

    public String extractRole(Claims claims) {
        Object role = claims.get("role");
        return role == null ? null : role.toString();
    }
}
