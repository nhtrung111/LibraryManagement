package com.library.exception;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** T4: every error comes back in one JSON shape, with no stack trace. */
class ErrorHandlerTest {

    @RestController
    static class BrokenController {
        @GetMapping("/business")
        String business() {
            throw new ApiException(ErrorCode.BOOK_NOT_BORROWED, "Reader RC0001 has no unreturned copy of B0099",
                    Map.of("barcode", "B0099"));
        }

        @GetMapping("/crash")
        String crash() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-13T02:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BrokenController())
            .setControllerAdvice(new GlobalExceptionHandler(new ErrorResponses(clock)))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build()))
            .build();

    @Test
    void T4_businessErrorHasStandardShape() throws Exception {
        mvc.perform(get("/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("BOOK_NOT_BORROWED"))
                .andExpect(jsonPath("$.message").value("Reader RC0001 has no unreturned copy of B0099"))
                .andExpect(jsonPath("$.details.barcode").value("B0099"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void T4_unexpectedErrorHidesDetails() throws Exception {
        mvc.perform(get("/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret internal detail"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }
}
