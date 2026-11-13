package com.library.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A loan slip (phiếu mượn). Created by Module 1; this module only reads it. */
@Entity
@Table(name = "loan_slip")
@Getter
@Setter
@NoArgsConstructor
public class LoanSlip extends BaseEntity {

    /** BR-1: due date = borrow date + 30 days. */
    public static final int LOAN_DAYS = 30;

    @Column(nullable = false, unique = true, length = 50)
    private String slipBarcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Column(nullable = false)
    private LocalDate borrowDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    public LoanSlip(String slipBarcode, Reader reader, LocalDate borrowDate) {
        this.slipBarcode = slipBarcode;
        this.reader = reader;
        this.borrowDate = borrowDate;
        this.dueDate = borrowDate.plusDays(LOAN_DAYS);
    }
}
