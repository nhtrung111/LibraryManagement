package com.library.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.BookDao;
import com.library.dto.BookResponse;
import com.library.exception.ApiException;
import com.library.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/** Book search and availability (US-6). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookDao bookDao;

    public List<BookResponse> search(String text) {
        return bookDao.search(text).stream().map(BookResponse::from).toList();
    }

    public BookResponse availability(String barcode) {
        return bookDao.findByBarcode(barcode)
                .map(BookResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOK_NOT_FOUND,
                        "No book with barcode " + barcode, Map.of("barcode", barcode)));
    }
}
