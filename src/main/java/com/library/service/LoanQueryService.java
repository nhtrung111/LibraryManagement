package com.library.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.AppUserDao;
import com.library.dao.LoanItemDao;
import com.library.dao.ReaderDao;
import com.library.dto.MyLoanResponse;
import com.library.dto.ReaderInfo;
import com.library.dto.ReaderLoansResponse;
import com.library.entity.AppUser;
import com.library.entity.LoanSlip;
import com.library.entity.Reader;
import com.library.exception.ApiException;
import com.library.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/** Read-only views of loans: the staff reader lookup (US-1) and a user's own loans (US-5). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanQueryService {

    private final ReaderDao readerDao;
    private final AppUserDao appUserDao;
    private final LoanItemDao loanItemDao;
    private final Clock clock;

    public ReaderLoansResponse readerLoans(String cardBarcode) {
        Reader reader = readerDao.findByCardBarcode(cardBarcode)
                .orElseThrow(() -> new ApiException(ErrorCode.READER_NOT_FOUND,
                        "No reader with card " + cardBarcode, Map.of("cardBarcode", cardBarcode)));
        LocalDate today = LocalDate.now(clock);
        return new ReaderLoansResponse(
                ReaderInfo.from(reader),
                loanItemDao.findBorrowedByReaderOrdered(reader.getId()).stream()
                        .map(i -> LoanViews.unreturned(i, today)).toList(),
                loanItemDao.findReturnedByReader(reader.getId()).stream()
                        .map(LoanViews::returned).toList());
    }

    public List<MyLoanResponse> myLoans(Long userId) {
        AppUser user = appUserDao.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        Reader reader = user.getReader();
        if (reader == null) {
            throw new ApiException(ErrorCode.READER_NOT_LINKED);
        }
        LocalDate today = LocalDate.now(clock);
        return loanItemDao.findBorrowedByReaderOrdered(reader.getId()).stream()
                .map(item -> toMyLoan(item.getLoanSlip(), item.getBook().getTitle(),
                        item.getBook().getBarcode(), today))
                .toList();
    }

    private MyLoanResponse toMyLoan(LoanSlip slip, String title, String barcode, LocalDate today) {
        long daysBorrowed = ChronoUnit.DAYS.between(slip.getBorrowDate(), today);
        long daysLeft = Math.max(0, ChronoUnit.DAYS.between(today, slip.getDueDate()));
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(slip.getDueDate(), today));
        return new MyLoanResponse(title, barcode, slip.getBorrowDate(), slip.getDueDate(),
                daysBorrowed, daysLeft, overdueDays, overdueDays > 0 ? "OVERDUE" : "ON_TIME");
    }
}
