package com.library.dto;

import java.math.BigDecimal;
import java.util.List;

/** Fine slip: late copies that were returned and paid for. */
public record FineSlipView(ReaderInfo reader, List<FineSlipLine> lines, BigDecimal totalFine) {
}
