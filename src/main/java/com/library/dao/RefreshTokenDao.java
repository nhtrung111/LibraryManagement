package com.library.dao;

import java.time.Instant;
import java.util.Optional;

import com.library.entity.RefreshToken;

public interface RefreshTokenDao {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Sets revokedAt on every not-yet-revoked token of the user. Returns how many rows changed. */
    int revokeAllActiveForUser(Long userId, Instant now);

    RefreshToken save(RefreshToken token);
}
