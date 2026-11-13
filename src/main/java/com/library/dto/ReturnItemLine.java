package com.library.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A scanned copy in a return result. fine is 0 when on time. */
public record ReturnItemLine(
        String bookCode,
        String title,
        String author,
        String bookBarcode,
        String slipBarcode,
        LocalDate borrowDate,
        LocalDate dueDate,
        boolean late,
        BigDecimal fine) {
}
