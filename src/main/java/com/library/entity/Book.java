package com.library.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row per title (đầu sách). The barcode identifies the title, not a physical copy (BR-3). */
@Entity
@Table(name = "book")
@Getter
@Setter
@NoArgsConstructor
public class Book extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 150)
    private String author;

    private Integer publishYear;

    /** VND, whole dong (constitution rule 4). */
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal coverPrice;

    @Column(nullable = false)
    private int totalCopies;

    @Column(nullable = false)
    private int availableCopies;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(length = 2000)
    private String description;

    public Book(String code, String title, String author, BigDecimal coverPrice,
                int totalCopies, int availableCopies, String barcode) {
        this.code = code;
        this.title = title;
        this.author = author;
        this.coverPrice = coverPrice;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.barcode = barcode;
    }
}
