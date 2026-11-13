package com.library.dto;

import java.util.List;

/** GET /api/staff/readers/{cardBarcode}/loans (US-1, AC-L1). */
public record ReaderLoansResponse(
        ReaderInfo reader,
        List<LoanItemView> unreturnedItems,
        List<LoanItemView> returnedItems) {
}
