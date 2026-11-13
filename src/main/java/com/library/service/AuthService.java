package com.library.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.AppUserDao;
import com.library.dto.LoginRequest;
import com.library.dto.TokenResponse;
import com.library.entity.AppUser;
import com.library.exception.ApiException;
import com.library.exception.ErrorCode;
import com.library.security.RefreshTokenService;

import lombok.RequiredArgsConstructor;

/** Login, refresh and logout (US-7, US-8, US-9). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserDao appUserDao;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    /** Same error for unknown user and wrong password, so attackers can't probe usernames (AC-A2). */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        AppUser user = appUserDao.findByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        return refreshTokenService.issue(user);
    }

    public TokenResponse refresh(String refreshToken) {
        return refreshTokenService.rotate(refreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
