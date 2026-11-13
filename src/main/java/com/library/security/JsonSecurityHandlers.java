package com.library.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.exception.ErrorCode;
import com.library.exception.ErrorResponses;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** 401 and 403 written in the standard error JSON, since they never reach the controller advice. */
@Component
@RequiredArgsConstructor
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ErrorResponses errors;

    /** Not logged in (or bad token) -> 401 with UNAUTHORIZED, TOKEN_EXPIRED or INVALID_TOKEN. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE);
        write(response, reason instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED);
    }

    /** Logged in but wrong role -> 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errors.of(code));
    }
}
