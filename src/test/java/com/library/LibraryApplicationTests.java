package com.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** T1 check: the app starts and the Clock bean exists. */
@SpringBootTest
class LibraryApplicationTests {

    @Autowired
    private Clock clock;

    @Test
    void T1_contextLoadsWithClock() {
        assertThat(clock).isNotNull();
    }
}
