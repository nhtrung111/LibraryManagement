package com.library.dto;

import com.library.entity.Book;

/** Book search and availability (US-6). status is AVAILABLE or OUT_OF_COPIES. */
public record BookResponse(
        String code,
        String title,
        String author,
        String barcode,
        int totalCopies,
        int availableCopies,
        String status) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getCode(), book.getTitle(), book.getAuthor(), book.getBarcode(),
                book.getTotalCopies(), book.getAvailableCopies(),
                book.getAvailableCopies() > 0 ? "AVAILABLE" : "OUT_OF_COPIES");
    }
}
