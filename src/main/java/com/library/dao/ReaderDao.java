package com.library.dao;

import java.util.Optional;

import com.library.entity.Reader;

public interface ReaderDao {

    Optional<Reader> findById(Long id);

    Optional<Reader> findByCardBarcode(String cardBarcode);

    Reader save(Reader reader);
}
