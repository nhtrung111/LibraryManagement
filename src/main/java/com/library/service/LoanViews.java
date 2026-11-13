package com.library.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.library.dto.FineSlipLine;
import com.library.dto.LoanItemView;
import com.library.dto.ReturnItemLine;
import com.library.entity.Book;
import com.library.entity.LoanItem;
import com.library.entity.LoanSlip;

/** Small shared helpers: the fine rule and entity-to-DTO conversion. */
final class LoanViews {

    /** BR-2: 20% of the cover price. */
    static final BigDecimal FINE_RATE = new BigDecimal("0.20");

    private LoanViews() {
    }

    /** BR-1: late only when returned strictly after the due date. */
    static boolean isLate(LoanSlip slip, LocalDate day) {
        return day.isAfter(slip.getDueDate());
    }

    /** BR-2: flat fine per late copy, whole dong, HALF_UP (constitution rule 4). */
    static BigDecimal fineFor(Book book) {
        return book.getCoverPrice().multiply(FINE_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    static LoanItemView unreturned(LoanItem item, LocalDate today) {
        LoanSlip slip = item.getLoanSlip();
        Book book = item.getBook();
        return new LoanItemView(book.getCode(), book.getTitle(), book.getAuthor(), book.getBarcode(),
                slip.getSlipBarcode(), slip.getBorrowDate(), slip.getDueDate(),
                null, isLate(slip, today), null);
    }

    static LoanItemView returned(LoanItem item) {
        LoanSlip slip = item.getLoanSlip();
        Book book = item.getBook();
        return new LoanItemView(book.getCode(), book.getTitle(), book.getAuthor(), book.getBarcode(),
                slip.getSlipBarcode(), slip.getBorrowDate(), slip.getDueDate(),
                item.getReturnDate(), isLate(slip, item.getReturnDate()), item.getFineAmount());
    }

    static ReturnItemLine returnLine(LoanItem item, boolean late, BigDecimal fine) {
        LoanSlip slip = item.getLoanSlip();
        Book book = item.getBook();
        return new ReturnItemLine(book.getCode(), book.getTitle(), book.getAuthor(), book.getBarcode(),
                slip.getSlipBarcode(), slip.getBorrowDate(), slip.getDueDate(), late, fine);
    }

    static FineSlipLine fineLine(LoanItem item, LocalDate returnDate, BigDecimal fine) {
        LoanSlip slip = item.getLoanSlip();
        Book book = item.getBook();
        return new FineSlipLine(book.getCode(), book.getTitle(), book.getAuthor(), book.getBarcode(),
                slip.getSlipBarcode(), slip.getBorrowDate(), slip.getDueDate(), returnDate, fine);
    }
}
