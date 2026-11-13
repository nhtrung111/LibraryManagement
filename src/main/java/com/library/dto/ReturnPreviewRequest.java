package com.library.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** POST /api/staff/returns/preview. The fine is treated as paid in a preview. */
public record ReturnPreviewRequest(
        @NotBlank String readerCardBarcode,
        @NotEmpty List<@NotBlank String> bookBarcodes) {
}
