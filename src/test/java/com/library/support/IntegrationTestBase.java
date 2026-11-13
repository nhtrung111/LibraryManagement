package com.library.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dao.AppUserDao;
import com.library.dao.BookDao;
import com.library.dao.LoanItemDao;
import com.library.dao.LoanSlipDao;
import com.library.dao.ReaderDao;
import com.library.entity.AppUser;
import com.library.entity.Book;
import com.library.entity.LoanItem;
import com.library.entity.LoanSlip;
import com.library.entity.LoanStatus;
import com.library.entity.Reader;
import com.library.entity.Role;

/**
 * Full app + in-memory H2. Each test runs in a transaction that is rolled back afterwards,
 * so tests never see each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClockConfig.class)
public abstract class IntegrationTestBase {

    public static final Instant NOW = Instant.parse("2026-11-13T02:00:00Z");
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    protected static final String PASSWORD = "pass12345";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected MutableClock clock;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected ReaderDao readerDao;
    @Autowired protected BookDao bookDao;
    @Autowired protected LoanSlipDao loanSlipDao;
    @Autowired protected LoanItemDao loanItemDao;
    @Autowired protected AppUserDao appUserDao;

    private int slipCounter;

    @BeforeEach
    void resetClock() {
        clock.set(NOW);
    }

    protected Reader reader(String code, String cardBarcode) {
        return readerDao.save(new Reader(code, "Reader " + code, cardBarcode));
    }

    protected Book book(String barcode, long coverPrice, int total, int available) {
        return bookDao.save(new Book("C-" + barcode, "Title " + barcode, "Author " + barcode,
                BigDecimal.valueOf(coverPrice), total, available, barcode));
    }

    /** One slip borrowed `daysAgo` days before TODAY, one copy per book given. */
    protected List<LoanItem> loan(Reader reader, int daysAgo, Book... books) {
        LoanSlip slip = loanSlipDao.save(
                new LoanSlip("LS-" + (++slipCounter), reader, TODAY.minusDays(daysAgo)));
        List<LoanItem> items = new ArrayList<>();
        for (Book b : books) {
            items.add(loanItemDao.save(new LoanItem(slip, b)));
        }
        return items;
    }

    protected LoanItem returnedEarlier(Reader reader, int daysAgo, Book book) {
        LoanItem item = loan(reader, daysAgo, book).get(0);
        item.setStatus(LoanStatus.RETURNED);
        item.setReturnDate(TODAY.minusDays(1));
        item.setFineAmount(BigDecimal.ZERO);
        return loanItemDao.save(item);
    }

    protected AppUser user(String username, Role role, Reader reader) {
        return appUserDao.save(new AppUser(username, passwordEncoder.encode(PASSWORD), role, reader));
    }

    /** Logs in through the real endpoint and returns the full token JSON. */
    protected JsonNode loginTokens(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    protected String bearer(String username) throws Exception {
        return "Bearer " + loginTokens(username).get("accessToken").asText();
    }

    protected String toJson(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
