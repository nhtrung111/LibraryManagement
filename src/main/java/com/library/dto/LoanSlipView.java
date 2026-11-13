package com.library.dto;

import java.util.List;

/** Updated loan slip: what the reader still holds after the return. */
public record LoanSlipView(ReaderInfo reader, List<LoanSlipLine> lines, int totalCopies) {
}
