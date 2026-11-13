package com.library.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.library.entity.Reader;
import com.library.entity.Role;
import com.library.support.IntegrationTestBase;

/** T9: what a normal user can see. */
class UserApiTest extends IntegrationTestBase {

    private Reader reader;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        reader = reader("R001", "RC0001");
        user("reader1", Role.USER, reader);
        userToken = bearer("reader1");
    }

    @Test
    void AC_U1_myLoanShowsDaysBorrowedAndLeft() throws Exception {
        loan(reader, 12, book("B1", 1, 1, 0));

        mvc.perform(get("/api/me/loans").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].daysBorrowed").value(12))
                .andExpect(jsonPath("$[0].daysLeft").value(18))
                .andExpect(jsonPath("$[0].overdueDays").value(0))
                .andExpect(jsonPath("$[0].status").value("ON_TIME"));
    }

    @Test
    void AC_U2_myLoanShowsOverdueDays() throws Exception {
        loan(reader, 33, book("B1", 1, 1, 0));

        mvc.perform(get("/api/me/loans").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].overdueDays").value(3))
                .andExpect(jsonPath("$[0].daysLeft").value(0))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"));
    }

    @Test
    void myLoansOnlyShowsMyOwnReader() throws Exception {
        Reader other = reader("R002", "RC0002");
        loan(other, 3, book("B9", 1, 1, 0));

        mvc.perform(get("/api/me/loans").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void userWithoutReaderGetsReaderNotLinked() throws Exception {
        user("lonely", Role.USER, null);

        mvc.perform(get("/api/me/loans").header(HttpHeaders.AUTHORIZATION, bearer("lonely")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("READER_NOT_LINKED"));
    }

    @Test
    void AC_U3_outOfCopiesForUserAndStaff() throws Exception {
        book("BX", 1, 2, 0);
        user("staff", Role.STAFF, null);

        for (String token : new String[] {userToken, bearer("staff")}) {
            mvc.perform(get("/api/books/BX/availability").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCopies").value(0))
                    .andExpect(jsonPath("$.totalCopies").value(2))
                    .andExpect(jsonPath("$.status").value("OUT_OF_COPIES"));
        }
    }

    @Test
    void searchBooksByText() throws Exception {
        book("B1", 1, 1, 1);
        book("B2", 1, 1, 1);

        mvc.perform(get("/api/books").param("q", "title b2").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].barcode").value("B2"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void unknownBookIsNotFound() throws Exception {
        mvc.perform(get("/api/books/NOPE/availability").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void AC_U5_noLoginIs401() throws Exception {
        for (String path : new String[] {"/api/books", "/api/me/loans", "/api/staff/readers/RC0001/loans"}) {
            mvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }
}
