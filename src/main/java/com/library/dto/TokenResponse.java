package com.library.dto;

/** Returned by login and refresh. expiresIn is the access token lifetime in seconds. */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
