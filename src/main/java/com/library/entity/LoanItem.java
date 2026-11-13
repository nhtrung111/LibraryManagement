package com.library.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One physical copy on a loan slip. Partial returns close individual rows (BR-6). */
@Entity
@Table(name = "loan_item")
@Getter
@Setter
@NoArgsConstructor
public class LoanItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_slip_id", nullable = false)
    private LoanSlip loanSlip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LoanStatus status = LoanStatus.BORROWED;

    private LocalDate returnDate;

    /** Null until returned. Zero when returned on time. */
    @Column(precision = 12, scale = 0)
    private BigDecimal fineAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_transaction_id")
    private ReturnTransaction returnTransaction;

    /** Optimistic lock: two staff returning the same copy at once -> CONCURRENT_UPDATE. */
    @Version
    private Long version;

    public LoanItem(LoanSlip loanSlip, Book book) {
        this.loanSlip = loanSlip;
        this.book = book;
    }
}
