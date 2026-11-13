package com.library.dto;

import java.time.LocalDate;

public record LoanSlipLine(
        String bookCode,
        String title,
        String author,
        String bookBarcode,
        String slipBarcode,
        LocalDate borrowDate,
        LocalDate dueDate,
        int quantity) {
}
