package com.library.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.AppUserDao;
import com.library.dao.BookDao;
import com.library.dao.LoanItemDao;
import com.library.dao.LoanSlipDao;
import com.library.dao.ReaderDao;
import com.library.entity.AppUser;
import com.library.entity.Book;
import com.library.entity.LoanItem;
import com.library.entity.LoanSlip;
import com.library.entity.Reader;
import com.library.entity.Role;

import lombok.RequiredArgsConstructor;

/**
 * T3: demo data for local runs. Runs once (skips if the "staff" user exists).
 * Turned off in tests with app.seed.enabled=false.
 * Reader RC0001 gets loans 5, 10, 20, 30, 31 and 33 days old, so every case can be tried by hand.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserDao appUserDao;
    private final ReaderDao readerDao;
    private final BookDao bookDao;
    private final LoanSlipDao loanSlipDao;
    private final LoanItemDao loanItemDao;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        if (appUserDao.findByUsername("staff").isPresent()) {
            return;
        }
        LocalDate today = LocalDate.now(clock);

        Reader an = readerDao.save(new Reader("R001", "Nguyễn Văn An", "RC0001"));
        an.setPhone("0901000001");
        an.setAddress("Hà Đông, Hà Nội");
        Reader binh = readerDao.save(new Reader("R002", "Trần Thị Bình", "RC0002"));

        appUserDao.save(new AppUser("staff", passwordEncoder.encode("staff123"), Role.STAFF, null));
        appUserDao.save(new AppUser("reader1", passwordEncoder.encode("reader123"), Role.USER, an));
        appUserDao.save(new AppUser("reader2", passwordEncoder.encode("reader123"), Role.USER, binh));

        Book cleanCode = book("S001", "Clean Code", "Robert C. Martin", 100_000, 5, "B0001");
        Book effectiveJava = book("S002", "Effective Java", "Joshua Bloch", 150_000, 3, "B0002");
        Book headFirst = book("S003", "Head First Java", "Kathy Sierra", 120_000, 2, "B0003");
        Book patterns = book("S004", "Design Patterns", "Erich Gamma", 200_000, 2, "B0004");
        Book refactoring = book("S005", "Refactoring", "Martin Fowler", 180_000, 1, "B0005");

        loan("LS0001", an, today.minusDays(33), cleanCode);            // late
        loan("LS0002", an, today.minusDays(31), effectiveJava);        // late
        loan("LS0003", an, today.minusDays(30), headFirst);            // due today, on time
        loan("LS0004", an, today.minusDays(20), cleanCode, patterns);
        loan("LS0005", an, today.minusDays(10), refactoring);          // last copy -> OUT_OF_COPIES
        loan("LS0006", an, today.minusDays(5), effectiveJava);

        log.info(">>> Seeded demo data. Logins: staff/staff123, reader1/reader123, reader2/reader123");
    }

    private Book book(String code, String title, String author, long price, int copies, String barcode) {
        return bookDao.save(new Book(code, title, author, BigDecimal.valueOf(price), copies, copies, barcode));
    }

    private void loan(String slipBarcode, Reader reader, LocalDate borrowDate, Book... books) {
        LoanSlip slip = loanSlipDao.save(new LoanSlip(slipBarcode, reader, borrowDate));
        for (Book book : books) {
            loanItemDao.save(new LoanItem(slip, book));
            book.setAvailableCopies(book.getAvailableCopies() - 1);
            bookDao.save(book);
        }
    }
}
