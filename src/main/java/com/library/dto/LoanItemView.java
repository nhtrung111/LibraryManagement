package com.library.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One copy in the staff reader lookup (US-1).
 * Unreturned copies: returnDate and fine are null; late = overdue as of today.
 * Returned copies: late = returned after the due date; fine = amount recorded.
 */
public record LoanItemView(
        String bookCode,
        String title,
        String author,
        String bookBarcode,
        String slipBarcode,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        boolean late,
        BigDecimal fine) {
}
