package com.library.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Constitution rule 5: all "today" logic uses this Clock, never LocalDate.now() with no argument.
 * Tests replace it with Clock.fixed(...) to make late-return cases deterministic.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
