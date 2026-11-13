package com.library.dao;

import java.util.List;
import java.util.Optional;

import com.library.entity.Book;

public interface BookDao {

    Optional<Book> findById(Long id);

    Optional<Book> findByBarcode(String barcode);

    /** Case-insensitive match on title, author or code. Blank text returns every book. Sorted by title. */
    List<Book> search(String text);

    Book save(Book book);
}
