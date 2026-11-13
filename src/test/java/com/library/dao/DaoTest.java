package com.library.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.library.dao.impl.AppUserDaoImpl;
import com.library.dao.impl.BookDaoImpl;
import com.library.dao.impl.LoanItemDaoImpl;
import com.library.dao.impl.LoanSlipDaoImpl;
import com.library.dao.impl.ReaderDaoImpl;
import com.library.dao.impl.RefreshTokenDaoImpl;
import com.library.dao.impl.ReturnTransactionDaoImpl;
import com.library.entity.AppUser;
import com.library.entity.Book;
import com.library.entity.LoanItem;
import com.library.entity.LoanSlip;
import com.library.entity.LoanStatus;
import com.library.entity.Reader;
import com.library.entity.RefreshToken;
import com.library.entity.Role;

import jakarta.persistence.EntityManager;

/** T2 check: entities map to tables and DAO queries return the right rows in the right order. */
@DataJpaTest
@Import({AppUserDaoImpl.class, ReaderDaoImpl.class, BookDaoImpl.class, LoanSlipDaoImpl.class,
        LoanItemDaoImpl.class, ReturnTransactionDaoImpl.class, RefreshTokenDaoImpl.class})
class DaoTest {

    @Autowired private ReaderDao readerDao;
    @Autowired private BookDao bookDao;
    @Autowired private LoanSlipDao loanSlipDao;
    @Autowired private LoanItemDao loanItemDao;
    @Autowired private AppUserDao appUserDao;
    @Autowired private RefreshTokenDao refreshTokenDao;
    @Autowired private EntityManager em;

    private Reader reader;
    private Book cleanCode;
    private Book effectiveJava;

    @BeforeEach
    void setUp() {
        reader = readerDao.save(new Reader("R001", "Nguyen Van A", "RC0001"));
        cleanCode = bookDao.save(new Book("S001", "Clean Code", "Robert C. Martin",
                new BigDecimal("100000"), 3, 1, "B0001"));
        effectiveJava = bookDao.save(new Book("S042", "Effective Java", "Joshua Bloch",
                new BigDecimal("150000"), 2, 1, "B0042"));
    }

    @Test
    void T2_borrowedItemsComeBackInBr4Order() {
        // Saved out of order on purpose: newest slip first.
        LoanSlip newest = loanSlipDao.save(new LoanSlip("LS-NEW", reader, LocalDate.of(2026, 10, 20)));
        LoanSlip oldestA = loanSlipDao.save(new LoanSlip("LS-OLD-A", reader, LocalDate.of(2026, 10, 1)));
        LoanSlip oldestB = loanSlipDao.save(new LoanSlip("LS-OLD-B", reader, LocalDate.of(2026, 10, 1)));

        LoanItem i1 = loanItemDao.save(new LoanItem(newest, cleanCode));
        LoanItem i2 = loanItemDao.save(new LoanItem(oldestA, cleanCode));
        LoanItem i3 = loanItemDao.save(new LoanItem(oldestA, effectiveJava));
        LoanItem i4 = loanItemDao.save(new LoanItem(oldestB, cleanCode));

        LoanItem returned = new LoanItem(oldestA, effectiveJava);
        returned.setStatus(LoanStatus.RETURNED);
        returned.setReturnDate(LocalDate.of(2026, 10, 15));
        returned.setFineAmount(BigDecimal.ZERO);
        loanItemDao.save(returned);

        // Another reader's loan must not show up.
        Reader other = readerDao.save(new Reader("R002", "Tran Thi B", "RC0002"));
        LoanSlip otherSlip = loanSlipDao.save(new LoanSlip("LS-OTHER", other, LocalDate.of(2026, 9, 1)));
        loanItemDao.save(new LoanItem(otherSlip, cleanCode));

        em.flush();
        em.clear();

        List<Long> ids = loanItemDao.findBorrowedByReaderOrdered(reader.getId())
                .stream().map(LoanItem::getId).toList();

        // Oct 1 slip A (lower id) items, then Oct 1 slip B, then Oct 20.
        assertThat(ids).containsExactly(i2.getId(), i3.getId(), i4.getId(), i1.getId());
        assertThat(loanItemDao.findReturnedByReader(reader.getId()))
                .extracting(LoanItem::getId).containsExactly(returned.getId());
    }

    @Test
    void T2_loanSlipDueDateIsBorrowDatePlus30() {
        LoanSlip slip = new LoanSlip("LS-1", reader, LocalDate.of(2026, 10, 1));
        assertThat(slip.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void T2_findByBarcodesAndSearch() {
        em.flush();
        em.clear();
        assertThat(readerDao.findByCardBarcode("RC0001")).isPresent();
        assertThat(readerDao.findByCardBarcode("NOPE")).isEmpty();
        assertThat(bookDao.findByBarcode("B0042")).get()
                .extracting(Book::getTitle).isEqualTo("Effective Java");
        assertThat(bookDao.search("bloch")).extracting(Book::getCode).containsExactly("S042");
        assertThat(bookDao.search(" ")).extracting(Book::getCode).containsExactly("S001", "S042");
    }

    @Test
    void T2_revokeAllActiveRefreshTokens() {
        AppUser user = appUserDao.save(new AppUser("staff1", "hash", Role.STAFF, null));
        Instant now = Instant.parse("2026-11-13T00:00:00Z");
        refreshTokenDao.save(new RefreshToken(user, "a".repeat(64), now, now.plusSeconds(3600)));
        refreshTokenDao.save(new RefreshToken(user, "b".repeat(64), now, now.plusSeconds(3600)));

        int changed = refreshTokenDao.revokeAllActiveForUser(user.getId(), now);

        assertThat(changed).isEqualTo(2);
        assertThat(refreshTokenDao.findByTokenHash("a".repeat(64))).get()
                .extracting(RefreshToken::isRevoked).isEqualTo(true);
        assertThat(appUserDao.findByUsername("staff1")).isPresent();
    }
}
