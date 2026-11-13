package com.library.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FineSlipLine(
        String bookCode,
        String title,
        String author,
        String bookBarcode,
        String slipBarcode,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        BigDecimal fine) {
}
