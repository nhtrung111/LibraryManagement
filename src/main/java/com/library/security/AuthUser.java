package com.library.security;

import com.library.entity.Role;

/** The logged-in user, read from the access token. Controllers get it via @AuthenticationPrincipal. */
public record AuthUser(Long id, String username, Role role) {
}
