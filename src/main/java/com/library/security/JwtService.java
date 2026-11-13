package com.library.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.library.entity.AppUser;
import com.library.entity.Role;
import com.library.exception.ApiException;
import com.library.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Creates and verifies access tokens (HS256, 15 minutes). Uses the injected Clock so tests can move time. */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-minutes}") long accessMinutes,
                      Clock clock) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = Duration.ofMinutes(accessMinutes);
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String createAccessToken(AppUser user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /** Throws TOKEN_EXPIRED or INVALID_TOKEN. */
    public AuthUser parse(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return new AuthUser(Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    Role.valueOf(claims.get("role", String.class)));
        } catch (ExpiredJwtException e) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }
}
