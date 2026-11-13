package com.library.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.entity.Role;
import com.library.support.IntegrationTestBase;

/** T10: login, refresh rotation, reuse detection, logout, expiry. */
class AuthApiTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        user("reader1", Role.USER, reader("R001", "RC0001"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("refreshToken", refreshToken))));
    }

    @Test
    void AC_A1_loginReturnsBothTokens() throws Exception {
        JsonNode tokens = loginTokens("reader1");

        org.assertj.core.api.Assertions.assertThat(tokens.get("accessToken").asText()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(tokens.get("refreshToken").asText()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(tokens.get("tokenType").asText()).isEqualTo("Bearer");
        org.assertj.core.api.Assertions.assertThat(tokens.get("expiresIn").asLong()).isEqualTo(900);

        mvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void AC_A2_wrongPasswordAndUnknownUserGiveSameError() throws Exception {
        for (Map<String, String> body : new Map[] {
                Map.of("username", "reader1", "password", "wrong-password"),
                Map.of("username", "ghost", "password", PASSWORD)}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Wrong username or password"));
        }
    }

    @Test
    void AC_A3_expiredAccessTokenIs401TokenExpired() throws Exception {
        String token = bearer("reader1");
        clock.advance(Duration.ofMinutes(16));

        mvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void AC_A4_refreshRotatesTokens() throws Exception {
        String oldRefresh = loginTokens("reader1").get("refreshToken").asText();

        String body = refresh(oldRefresh)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String newRefresh = json.readTree(body).get("refreshToken").asText();

        org.assertj.core.api.Assertions.assertThat(newRefresh).isNotEqualTo(oldRefresh);
        refresh(newRefresh).andExpect(status().isOk());
    }

    @Test
    void AC_A5_reusingRotatedTokenRevokesEverything() throws Exception {
        String first = loginTokens("reader1").get("refreshToken").asText();
        String second = json.readTree(refresh(first).andReturn().getResponse().getContentAsString())
                .get("refreshToken").asText();

        refresh(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

        // The legitimate newer token was revoked too.
        refresh(second)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void AC_A6_logoutThenRefreshIsInvalid() throws Exception {
        String refreshToken = loginTokens("reader1").get("refreshToken").asText();

        mvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void expiredRefreshTokenIsInvalid() throws Exception {
        String refreshToken = loginTokens("reader1").get("refreshToken").asText();
        clock.advance(Duration.ofDays(8));

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void garbageAccessTokenIsInvalidToken() throws Exception {
        mvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }
}
