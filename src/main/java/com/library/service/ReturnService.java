package com.library.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.AppUserDao;
import com.library.dao.BookDao;
import com.library.dao.LoanItemDao;
import com.library.dao.ReaderDao;
import com.library.dao.ReturnTransactionDao;
import com.library.dto.FineSlipLine;
import com.library.dto.FineSlipView;
import com.library.dto.LoanSlipLine;
import com.library.dto.LoanSlipView;
import com.library.dto.ReaderInfo;
import com.library.dto.ReturnConfirmRequest;
import com.library.dto.ReturnItemLine;
import com.library.dto.ReturnPreviewRequest;
import com.library.dto.ReturnResult;
import com.library.entity.Book;
import com.library.entity.LoanItem;
import com.library.entity.LoanSlip;
import com.library.entity.LoanStatus;
import com.library.entity.Reader;
import com.library.entity.ReturnTransaction;
import com.library.exception.ApiException;
import com.library.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Book returns (US-2, US-3, US-4). Preview and confirm run the same process() method,
 * so a preview always shows exactly what a confirm would do (AC-R9).
 * Steps are numbered as in plan.md "Return algorithm".
 */
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReaderDao readerDao;
    private final BookDao bookDao;
    private final LoanItemDao loanItemDao;
    private final ReturnTransactionDao returnTransactionDao;
    private final AppUserDao appUserDao;
    private final Clock clock;

    /** Nothing is saved. Fines are treated as paid. */
    @Transactional(readOnly = true)
    public ReturnResult preview(ReturnPreviewRequest request) {
        return process(request.readerCardBarcode(), request.bookBarcodes(), true, false, null);
    }

    @Transactional
    public ReturnResult confirm(ReturnConfirmRequest request, Long staffUserId) {
        return process(request.readerCardBarcode(), request.bookBarcodes(),
                request.finePaid(), true, staffUserId);
    }

    private ReturnResult process(String cardBarcode, List<String> scannedBarcodes,
                                 boolean finePaid, boolean commit, Long staffUserId) {
        // 1. Find the reader.
        Reader reader = readerDao.findByCardBarcode(cardBarcode)
                .orElseThrow(() -> new ApiException(ErrorCode.READER_NOT_FOUND,
                        "No reader with card " + cardBarcode, Map.of("cardBarcode", cardBarcode)));

        // 2. Unreturned copies, oldest first (BR-4), as one queue per book barcode.
        List<LoanItem> borrowed = loanItemDao.findBorrowedByReaderOrdered(reader.getId());
        Map<String, Deque<LoanItem>> queues = new LinkedHashMap<>();
        for (LoanItem item : borrowed) {
            queues.computeIfAbsent(item.getBook().getBarcode(), k -> new ArrayDeque<>()).add(item);
        }

        // 3. Match each scan to the oldest copy of that title. Any bad scan rejects everything (BR-5).
        List<LoanItem> selected = new ArrayList<>();
        for (String barcode : scannedBarcodes) {
            Deque<LoanItem> queue = queues.get(barcode);
            if (queue == null) {
                if (bookDao.findByBarcode(barcode).isEmpty()) {
                    throw new ApiException(ErrorCode.BOOK_NOT_FOUND,
                            "No book with barcode " + barcode, Map.of("barcode", barcode));
                }
                throw new ApiException(ErrorCode.BOOK_NOT_BORROWED,
                        "Reader " + cardBarcode + " has no unreturned copy of " + barcode,
                        Map.of("barcode", barcode));
            }
            LoanItem next = queue.pollFirst();
            if (next == null) {
                throw new ApiException(ErrorCode.TOO_MANY_COPIES,
                        "Reader " + cardBarcode + " holds fewer copies of " + barcode + " than were scanned",
                        Map.of("barcode", barcode));
            }
            selected.add(next);
        }

        // 4. Late check and fine (BR-1, BR-2).
        LocalDate today = LocalDate.now(clock);
        List<LoanItem> toReturn = new ArrayList<>();
        List<ReturnItemLine> returnedLines = new ArrayList<>();
        List<ReturnItemLine> keptLines = new ArrayList<>();
        List<FineSlipLine> fineLines = new ArrayList<>();
        Map<LoanItem, BigDecimal> fines = new IdentityHashMap<>();
        BigDecimal totalFine = BigDecimal.ZERO;

        for (LoanItem item : selected) {
            boolean late = LoanViews.isLate(item.getLoanSlip(), today);
            BigDecimal fine = late ? LoanViews.fineFor(item.getBook()) : BigDecimal.ZERO;

            // 5. Unpaid fine: late copies stay on loan (BR-7).
            if (late && !finePaid) {
                keptLines.add(LoanViews.returnLine(item, true, fine));
                continue;
            }
            toReturn.add(item);
            fines.put(item, fine);
            returnedLines.add(LoanViews.returnLine(item, late, fine));
            if (late) {
                fineLines.add(LoanViews.fineLine(item, today, fine));
                totalFine = totalFine.add(fine);
            }
        }

        // 6. Save: one transaction row, each copy marked returned, stock +1 per copy (BR-8).
        Long returnId = null;
        if (commit && !toReturn.isEmpty()) {
            ReturnTransaction tx = new ReturnTransaction();
            tx.setReader(reader);
            tx.setStaffUser(staffUserId == null ? null : appUserDao.findById(staffUserId).orElse(null));
            tx.setReturnDate(today);
            tx.setTotalFine(totalFine);
            tx.setFinePaid(finePaid);
            returnTransactionDao.save(tx);
            returnId = tx.getId();

            for (LoanItem item : toReturn) {
                item.setStatus(LoanStatus.RETURNED);
                item.setReturnDate(today);
                item.setFineAmount(fines.get(item));
                item.setReturnTransaction(tx);
                loanItemDao.save(item);

                Book book = item.getBook();
                book.setAvailableCopies(book.getAvailableCopies() + 1);
                bookDao.save(book);
            }
        }

        // 7. Build the slips from what is left.
        ReaderInfo readerInfo = ReaderInfo.from(reader);
        Set<LoanItem> returnedSet = Collections.newSetFromMap(new IdentityHashMap<>());
        returnedSet.addAll(toReturn);
        List<LoanItem> remaining = borrowed.stream().filter(i -> !returnedSet.contains(i)).toList();

        LoanSlipView loanSlip = remaining.isEmpty()
                ? null
                : new LoanSlipView(readerInfo, groupLines(remaining), remaining.size());
        FineSlipView fineSlip = fineLines.isEmpty()
                ? null
                : new FineSlipView(readerInfo, fineLines, totalFine);

        return new ReturnResult(returnId, today, readerInfo, returnedLines, keptLines, loanSlip, fineSlip);
    }

    /** One line per book + slip + borrow date, with a quantity (open question 3 in tasks.md). */
    private List<LoanSlipLine> groupLines(List<LoanItem> items) {
        Map<String, LoanSlipLine> lines = new LinkedHashMap<>();
        for (LoanItem item : items) {
            Book book = item.getBook();
            LoanSlip slip = item.getLoanSlip();
            String key = book.getId() + "|" + slip.getId() + "|" + slip.getBorrowDate();
            LoanSlipLine existing = lines.get(key);
            int quantity = existing == null ? 1 : existing.quantity() + 1;
            lines.put(key, new LoanSlipLine(book.getCode(), book.getTitle(), book.getAuthor(),
                    book.getBarcode(), slip.getSlipBarcode(), slip.getBorrowDate(), slip.getDueDate(),
                    quantity));
        }
        return List.copyOf(lines.values());
    }
}
