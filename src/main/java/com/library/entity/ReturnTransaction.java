package com.library.entity;

import java.math.BigDecimal;
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

/** One confirmed return, so the fine slip can be rebuilt later. */
@Entity
@Table(name = "return_transaction")
@Getter
@Setter
@NoArgsConstructor
public class ReturnTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_user_id")
    private AppUser staffUser;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal totalFine = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean finePaid;
}
