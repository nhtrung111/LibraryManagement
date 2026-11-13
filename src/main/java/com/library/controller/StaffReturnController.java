package com.library.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.ReaderLoansResponse;
import com.library.dto.ReturnConfirmRequest;
import com.library.dto.ReturnPreviewRequest;
import com.library.dto.ReturnResult;
import com.library.security.AuthUser;
import com.library.service.LoanQueryService;
import com.library.service.ReturnService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** STAFF only (SecurityConfig). US-1 to US-4. */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffReturnController {

    private final LoanQueryService loanQueryService;
    private final ReturnService returnService;

    @GetMapping("/readers/{cardBarcode}/loans")
    public ReaderLoansResponse readerLoans(@PathVariable String cardBarcode) {
        return loanQueryService.readerLoans(cardBarcode);
    }

    @PostMapping("/returns/preview")
    public ReturnResult preview(@Valid @RequestBody ReturnPreviewRequest request) {
        return returnService.preview(request);
    }

    @PostMapping("/returns")
    public ReturnResult confirm(@Valid @RequestBody ReturnConfirmRequest request,
                                @AuthenticationPrincipal AuthUser staff) {
        return returnService.confirm(request, staff.id());
    }
}
