package com.library.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.RefreshTokenDao;
import com.library.dto.TokenResponse;
import com.library.entity.AppUser;
import com.library.entity.RefreshToken;
import com.library.exception.ApiException;
import com.library.exception.ErrorCode;

/**
 * Refresh tokens: random, stored only as a SHA-256 hash, rotated on every use (plan: Security).
 * Reusing a token that was already rotated is treated as theft: every token of that user is revoked.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenDao refreshTokenDao;
    private final JwtService jwtService;
    private final Clock clock;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenDao refreshTokenDao, JwtService jwtService, Clock clock,
                               @Value("${jwt.refresh-token-days}") long refreshDays) {
        this.refreshTokenDao = refreshTokenDao;
        this.jwtService = jwtService;
        this.clock = clock;
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    /** New access + refresh pair for a user who just proved who they are. */
    @Transactional
    public TokenResponse issue(AppUser user) {
        String raw = newRawToken();
        save(user, raw);
        return tokens(user, raw);
    }

    /**
     * Steps from plan.md "Refresh with rotation".
     * noRollbackFor keeps the "revoke everything" write even though we then throw.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenDao.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));

        if (current.isRevoked() && current.getReplacedBy() != null) {
            // Rotated earlier and used again: someone may have stolen it (AC-A5).
            refreshTokenDao.revokeAllActiveForUser(current.getUser().getId(), now);
            throw new ApiException(ErrorCode.REFRESH_TOKEN_REUSED);
        }
        if (current.isRevoked() || current.getExpiresAt().isBefore(now)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN); // logged out or expired (AC-A6)
        }

        AppUser user = current.getUser();
        String raw = newRawToken();
        RefreshToken next = save(user, raw);
        current.setRevokedAt(now);
        current.setReplacedBy(next);
        refreshTokenDao.save(current);
        return tokens(user, raw);
    }

    /** Revokes the token. Unknown or already revoked tokens are ignored, so logout always succeeds. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenDao.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevokedAt(clock.instant());
                refreshTokenDao.save(token);
            }
        });
    }

    private RefreshToken save(AppUser user, String raw) {
        Instant now = clock.instant();
        return refreshTokenDao.save(new RefreshToken(user, hash(raw), now, now.plus(refreshTtl)));
    }

    private TokenResponse tokens(AppUser user, String rawRefresh) {
        return new TokenResponse(jwtService.createAccessToken(user), rawRefresh, "Bearer",
                jwtService.accessTtlSeconds());
    }

    private String newRawToken() {
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
