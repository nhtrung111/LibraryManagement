package com.library.dao.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.RefreshTokenDao;
import com.library.entity.RefreshToken;

@Repository
public class RefreshTokenDaoImpl extends AbstractJpaDao<RefreshToken> implements RefreshTokenDao {

    public RefreshTokenDaoImpl() {
        super(RefreshToken.class);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return first(em.createQuery("""
                        select t from RefreshToken t
                        join fetch t.user
                        where t.tokenHash = :hash
                        """, RefreshToken.class)
                .setParameter("hash", tokenHash));
    }

    @Override
    @Transactional
    public int revokeAllActiveForUser(Long userId, Instant now) {
        // Bulk update skips the persistence context, so flush pending changes first
        // and clear afterwards so later reads see the new revokedAt values.
        em.flush();
        int changed = em.createQuery("""
                        update RefreshToken t set t.revokedAt = :now
                        where t.user.id = :userId and t.revokedAt is null
                        """)
                .setParameter("now", now)
                .setParameter("userId", userId)
                .executeUpdate();
        em.clear();
        return changed;
    }
}
