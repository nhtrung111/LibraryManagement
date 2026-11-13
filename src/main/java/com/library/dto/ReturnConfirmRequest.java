package com.library.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** POST /api/staff/returns. finePaid=false keeps late copies on loan (BR-7). */
public record ReturnConfirmRequest(
        @NotBlank String readerCardBarcode,
        @NotEmpty List<@NotBlank String> bookBarcodes,
        @NotNull Boolean finePaid) {
}
