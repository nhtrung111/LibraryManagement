package com.library.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A library card holder (bạn đọc). */
@Entity
@Table(name = "reader")
@Getter
@Setter
@NoArgsConstructor
public class Reader extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private LocalDate birthDate;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, unique = true, length = 50)
    private String cardBarcode;

    public Reader(String code, String name, String cardBarcode) {
        this.code = code;
        this.name = name;
        this.cardBarcode = cardBarcode;
    }
}
