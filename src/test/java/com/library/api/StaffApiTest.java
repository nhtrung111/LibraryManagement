package com.library.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.library.entity.Book;
import com.library.entity.LoanItem;
import com.library.entity.LoanStatus;
import com.library.entity.Reader;
import com.library.entity.Role;
import com.library.support.IntegrationTestBase;

/** T5 + T8: staff endpoints over HTTP, with a real login token. */
class StaffApiTest extends IntegrationTestBase {

    private Reader reader;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        reader = reader("R001", "RC0001");
        user("staff", Role.STAFF, null);
        staffToken = bearer("staff");
    }

    @Test
    void AC_L1_lookupShowsUnreturnedAndReturned() throws Exception {
        loan(reader, 5, book("B1", 1, 1, 0), book("B2", 1, 1, 0));
        returnedEarlier(reader, 40, book("B3", 1, 1, 1));
        returnedEarlier(reader, 40, book("B4", 1, 1, 1));
        returnedEarlier(reader, 40, book("B5", 1, 1, 1));

        mvc.perform(get("/api/staff/readers/RC0001/loans").header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reader.code").value("R001"))
                .andExpect(jsonPath("$.reader.cardBarcode").value("RC0001"))
                .andExpect(jsonPath("$.unreturnedItems", hasSize(2)))
                .andExpect(jsonPath("$.returnedItems", hasSize(3)));
    }

    @Test
    void AC_L2_unknownCardIsNotFound() throws Exception {
        mvc.perform(get("/api/staff/readers/NOPE/loans").header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("READER_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void AC_R9_previewChangesNothing() throws Exception {
        Book b = book("B1", 100_000, 1, 0);
        LoanItem item = loan(reader, 31, b).get(0);

        mvc.perform(post("/api/staff/returns/preview")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("readerCardBarcode", "RC0001", "bookBarcodes", List.of("B1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnId", nullValue()))
                .andExpect(jsonPath("$.returnedItems", hasSize(1)))
                .andExpect(jsonPath("$.fineSlip.totalFine").value(20000))
                .andExpect(jsonPath("$.loanSlip", nullValue()));

        assertThat(loanItemDao.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(LoanStatus.BORROWED);
        assertThat(bookDao.findByBarcode("B1").orElseThrow().getAvailableCopies()).isZero();
    }

    @Test
    void AC_R10_emptyScanListIsValidationError() throws Exception {
        String body = toJson(Map.of("readerCardBarcode", "RC0001", "bookBarcodes", List.of(), "finePaid", true));
        for (String path : List.of("/api/staff/returns/preview", "/api/staff/returns")) {
            mvc.perform(post(path)
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details.bookBarcodes").exists());
        }
    }

    @Test
    void confirmOverHttpReturnsSlips() throws Exception {
        loan(reader, 31, book("B1", 100_000, 1, 0));
        loan(reader, 5, book("B2", 50_000, 1, 0));

        mvc.perform(post("/api/staff/returns")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("readerCardBarcode", "RC0001",
                                "bookBarcodes", List.of("B1"), "finePaid", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnId", notNullValue()))
                .andExpect(jsonPath("$.returnDate").value("2026-11-13"))
                .andExpect(jsonPath("$.fineSlip.lines[0].fine").value(20000))
                .andExpect(jsonPath("$.loanSlip.totalCopies").value(1))
                .andExpect(jsonPath("$.loanSlip.lines[0].bookBarcode").value("B2"));
    }

    @Test
    void missingFinePaidIsValidationError() throws Exception {
        mvc.perform(post("/api/staff/returns")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("readerCardBarcode", "RC0001", "bookBarcodes", List.of("B1")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.finePaid").exists());
    }

    @Test
    void AC_U4_normalUserCannotUseStaffEndpoints() throws Exception {
        user("reader1", Role.USER, reader);
        String userToken = bearer("reader1");

        mvc.perform(post("/api/staff/returns")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("readerCardBarcode", "RC0001",
                                "bookBarcodes", List.of("B1"), "finePaid", true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/staff/readers/RC0001/loans").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }
}
