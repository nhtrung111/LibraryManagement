package com.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.library.dto.ReturnConfirmRequest;
import com.library.dto.ReturnResult;
import com.library.entity.Book;
import com.library.entity.LoanItem;
import com.library.entity.LoanStatus;
import com.library.entity.Reader;
import com.library.exception.ApiException;
import com.library.exception.ErrorCode;
import com.library.support.IntegrationTestBase;

/** T6 + T7: the return rules, tested through ReturnService with a fixed "today" of 2026-11-13. */
class ReturnServiceTest extends IntegrationTestBase {

    @Autowired private ReturnService returnService;

    private Reader reader;

    @BeforeEach
    void setUp() {
        reader = reader("R001", "RC0001");
    }

    private ReturnResult confirm(boolean finePaid, String... barcodes) {
        return returnService.confirm(new ReturnConfirmRequest("RC0001", List.of(barcodes), finePaid), null);
    }

    @Test
    void AC_R1_returnAllThreeOnTime() {
        Book a = book("B1", 100_000, 2, 1);
        Book b = book("B2", 100_000, 2, 1);
        Book c = book("B3", 100_000, 2, 1);
        List<LoanItem> items = loan(reader, 10, a, b, c);

        ReturnResult result = confirm(true, "B1", "B2", "B3");

        assertThat(result.returnId()).isNotNull();
        assertThat(result.returnedItems()).hasSize(3);
        assertThat(result.loanSlip()).isNull();
        assertThat(result.fineSlip()).isNull();
        assertThat(items).allSatisfy(i -> {
            assertThat(i.getStatus()).isEqualTo(LoanStatus.RETURNED);
            assertThat(i.getReturnDate()).isEqualTo(TODAY);
            assertThat(i.getFineAmount()).isEqualByComparingTo("0");
        });
        assertThat(List.of(a, b, c)).allSatisfy(book -> assertThat(book.getAvailableCopies()).isEqualTo(2));
    }

    @Test
    void AC_R2_partialReturnLeavesThreeOnLoanSlip() {
        loan(reader, 5, book("B1", 1, 1, 0), book("B2", 1, 1, 0), book("B3", 1, 1, 0), book("B4", 1, 1, 0));

        ReturnResult result = confirm(true, "B1");

        assertThat(result.returnedItems()).hasSize(1);
        assertThat(result.loanSlip()).isNotNull();
        assertThat(result.loanSlip().totalCopies()).isEqualTo(3);
        assertThat(result.loanSlip().lines()).extracting("bookBarcode").containsExactly("B2", "B3", "B4");
    }

    @Test
    void AC_R3_lateBookCreatesFine() {
        LoanItem item = loan(reader, 31, book("B1", 100_000, 1, 0)).get(0);

        ReturnResult result = confirm(true, "B1");

        assertThat(item.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(item.getFineAmount()).isEqualByComparingTo("20000");
        assertThat(result.returnedItems().get(0).late()).isTrue();
        assertThat(result.fineSlip()).isNotNull();
        assertThat(result.fineSlip().lines()).hasSize(1);
        assertThat(result.fineSlip().lines().get(0).fine()).isEqualByComparingTo("20000");
        assertThat(result.fineSlip().lines().get(0).returnDate()).isEqualTo(TODAY);
        assertThat(result.fineSlip().totalFine()).isEqualByComparingTo("20000");
    }

    @Test
    void AC_R4_exactly30DaysIsNotLate() {
        loan(reader, 30, book("B1", 100_000, 1, 0));

        ReturnResult result = confirm(true, "B1");

        assertThat(result.returnedItems().get(0).late()).isFalse();
        assertThat(result.returnedItems().get(0).fine()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.fineSlip()).isNull();
    }

    @Test
    void AC_R5_unpaidFineKeepsLateCopyOnLoan() {
        Book lateBook = book("B1", 100_000, 1, 0);
        Book onTimeBook = book("B2", 100_000, 1, 0);
        LoanItem late = loan(reader, 31, lateBook).get(0);
        LoanItem onTime = loan(reader, 5, onTimeBook).get(0);

        ReturnResult result = confirm(false, "B1", "B2");

        assertThat(onTime.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(late.getStatus()).isEqualTo(LoanStatus.BORROWED);
        assertThat(late.getFineAmount()).isNull();
        assertThat(lateBook.getAvailableCopies()).isZero();
        assertThat(onTimeBook.getAvailableCopies()).isEqualTo(1);
        assertThat(result.returnedItems()).extracting("bookBarcode").containsExactly("B2");
        assertThat(result.keptOnLoanItems()).extracting("bookBarcode").containsExactly("B1");
        assertThat(result.loanSlip().lines()).extracting("bookBarcode").containsExactly("B1");
        assertThat(result.fineSlip()).isNull();
    }

    @Test
    void AC_R6_bookNotBorrowedRejectsEverything() {
        LoanItem held = loan(reader, 5, book("B1", 1, 1, 0)).get(0);
        book("BX", 1, 1, 1);

        assertThatThrownBy(() -> confirm(true, "B1", "BX"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_BORROWED);
        assertThat(held.getStatus()).isEqualTo(LoanStatus.BORROWED);
    }

    @Test
    void AC_R7_scanningMoreCopiesThanHeldRejectsEverything() {
        LoanItem held = loan(reader, 5, book("BX", 1, 1, 0)).get(0);

        assertThatThrownBy(() -> confirm(true, "BX", "BX"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.TOO_MANY_COPIES);
        assertThat(held.getStatus()).isEqualTo(LoanStatus.BORROWED);
    }

    @Test
    void AC_R8_oldestCopyIsReturnedFirst() {
        Book x = book("BX", 1, 2, 0);
        LoanItem newer = loan(reader, 5, x).get(0);
        LoanItem older = loan(reader, 20, x).get(0);

        confirm(true, "BX");

        assertThat(older.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(newer.getStatus()).isEqualTo(LoanStatus.BORROWED);
    }

    @Test
    void unknownBarcodeIsBookNotFound() {
        loan(reader, 5, book("B1", 1, 1, 0));

        assertThatThrownBy(() -> confirm(true, "NOPE"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void unknownReaderIsReaderNotFound() {
        assertThatThrownBy(() -> returnService.confirm(
                        new ReturnConfirmRequest("NOPE", List.of("B1"), true), null))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.READER_NOT_FOUND);
    }

    @Test
    void sameTitleSameSlipIsOneLineWithQuantity() {
        Book x = book("BX", 1, 3, 0);
        loan(reader, 5, x, x, x);

        ReturnResult result = confirm(true, "BX");

        assertThat(result.loanSlip().lines()).hasSize(1);
        assertThat(result.loanSlip().lines().get(0).quantity()).isEqualTo(2);
        assertThat(result.loanSlip().totalCopies()).isEqualTo(2);
    }
}
