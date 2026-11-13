package com.library.support;

import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the real clock: "today" is always 13 Nov 2026 (09:00 in Hanoi) unless a test moves it. */
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(IntegrationTestBase.NOW, ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
