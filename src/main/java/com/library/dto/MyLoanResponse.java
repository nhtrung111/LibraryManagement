package com.library.dto;

import java.time.LocalDate;

/** GET /api/me/loans, one entry per unreturned copy (US-5). status is ON_TIME or OVERDUE. */
public record MyLoanResponse(
        String title,
        String bookBarcode,
        LocalDate borrowDate,
        LocalDate dueDate,
        long daysBorrowed,
        long daysLeft,
        long overdueDays,
        String status) {
}
