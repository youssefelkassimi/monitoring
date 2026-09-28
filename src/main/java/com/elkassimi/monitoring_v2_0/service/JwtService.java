package com.elkassimi.monitoring_v2_0.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration userTokenLifetime;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.user-token-lifetime:PT8H}") Duration userTokenLifetime) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("app.security.jwt-secret must contain at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.userTokenLifetime = userTokenLifetime;
    }

    public String createUserToken(String userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .claim("kind", "user")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(userTokenLifetime)))
                .signWith(key)
                .compact();
    }

    public String createAgentToken(String agentId, Instant expiresAt) {
        return Jwts.builder()
                .subject(agentId)
                .claim("kind", "agent")
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
