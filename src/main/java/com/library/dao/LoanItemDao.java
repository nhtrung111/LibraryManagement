package com.library.dao;

import java.util.List;
import java.util.Optional;

import com.library.entity.LoanItem;

public interface LoanItemDao {

    Optional<LoanItem> findById(Long id);

    /**
     * The reader's BORROWED copies in BR-4 order: borrow date, then slip id, then item id.
     * Slip, book and reader are fetched in the same query.
     */
    List<LoanItem> findBorrowedByReaderOrdered(Long readerId);

    /** The reader's RETURNED copies, newest return first. */
    List<LoanItem> findReturnedByReader(Long readerId);

    LoanItem save(LoanItem item);
}
