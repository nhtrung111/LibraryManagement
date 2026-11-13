package com.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.library.dao.AppUserDao;
import com.library.dao.BookDao;
import com.library.dao.LoanItemDao;
import com.library.dao.ReaderDao;

/** T3: demo data loads on startup and running the seeder twice adds nothing. Uses its own H2 database. */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:seedtest;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"})
class DataSeederTest {

    @Autowired private DataSeeder seeder;
    @Autowired private AppUserDao appUserDao;
    @Autowired private ReaderDao readerDao;
    @Autowired private LoanItemDao loanItemDao;
    @Autowired private BookDao bookDao;

    @Test
    void T3_seedDataLoadedOnceOnly() {
        assertThat(appUserDao.findByUsername("staff")).isPresent();
        assertThat(appUserDao.findByUsername("reader1")).isPresent();
        Long readerId = readerDao.findByCardBarcode("RC0001").orElseThrow().getId();
        assertThat(loanItemDao.findBorrowedByReaderOrdered(readerId)).hasSize(7);
        assertThat(bookDao.findByBarcode("B0005").orElseThrow().getAvailableCopies()).isZero();

        seeder.run();

        assertThat(loanItemDao.findBorrowedByReaderOrdered(readerId)).hasSize(7);
    }
}
