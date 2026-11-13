package com.library.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.MyLoanResponse;
import com.library.security.AuthUser;
import com.library.service.LoanQueryService;

import lombok.RequiredArgsConstructor;

/** USER only. The reader always comes from the token, never from a parameter. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final LoanQueryService loanQueryService;

    @GetMapping("/loans")
    public List<MyLoanResponse> myLoans(@AuthenticationPrincipal AuthUser user) {
        return loanQueryService.myLoans(user.id());
    }
}
