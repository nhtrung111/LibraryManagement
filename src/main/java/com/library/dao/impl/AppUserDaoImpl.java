package com.library.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.library.dao.AppUserDao;
import com.library.entity.AppUser;

@Repository
public class AppUserDaoImpl extends AbstractJpaDao<AppUser> implements AppUserDao {

    public AppUserDaoImpl() {
        super(AppUser.class);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return first(em.createQuery(
                        "select u from AppUser u where u.username = :username", AppUser.class)
                .setParameter("username", username));
    }
}
