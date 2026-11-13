package com.library.dao;

import java.util.Optional;

import com.library.entity.AppUser;

public interface AppUserDao {

    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByUsername(String username);

    AppUser save(AppUser user);
}
