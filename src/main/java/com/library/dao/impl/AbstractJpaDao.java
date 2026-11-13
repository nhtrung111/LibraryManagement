package com.library.dao.impl;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.library.entity.BaseEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Shared findById/save for every DAO. Writes join the caller's transaction
 * (services are @Transactional per constitution rule 6), or start one if called alone.
 */
public abstract class AbstractJpaDao<T extends BaseEntity> {

    @PersistenceContext
    protected EntityManager em;

    private final Class<T> type;

    protected AbstractJpaDao(Class<T> type) {
        this.type = type;
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(em.find(type, id));
    }

    @Transactional
    public T save(T entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    /**
     * First result or empty; avoids NoResultException from getSingleResult().
     * Uses getResultList (not getResultStream): a stream is read lazily and fails
     * if the EntityManager closes first, which happens when called outside a transaction.
     */
    protected Optional<T> first(TypedQuery<T> query) {
        return query.setMaxResults(1).getResultList().stream().findFirst();
    }
}
