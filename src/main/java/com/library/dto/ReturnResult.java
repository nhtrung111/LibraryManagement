package com.library.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Shared by preview and confirm (AC-R9).
 * returnId is null in a preview or when nothing was returned.
 * loanSlip is null when the reader holds nothing; fineSlip is null when no paid late copy.
 */
public record ReturnResult(
        Long returnId,
        LocalDate returnDate,
        ReaderInfo reader,
        List<ReturnItemLine> returnedItems,
        List<ReturnItemLine> keptOnLoanItems,
        LoanSlipView loanSlip,
        FineSlipView fineSlip) {
}
